#  This file is part of Invision Archive Tools (IAT).
#
#  Copyright (C) 2025 Mark Fisher
#
#  This program is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with this program. If not, see https://www.gnu.org/licenses/.

from bs4 import BeautifulSoup, Tag
from datetime import datetime
from ..worker.ic import IC
from .. import util

MAX_QUOTE_DEPTH = 20

class ContentConverter:
	def __init__(self, html: str, ic: IC):
		self.ic = ic
		self.soup = BeautifulSoup(html, "html.parser")

	def convert(self) -> str:
		# Convert quotes
		self.convert_quotes()
		# Convert relative resources (like images)
		self.convert_relative_resources()
		# Additional conversions can be added here
		return str(self.soup)

	def convert_quotes(self) -> str:
		quotes = self.soup.find_all("blockquote", class_="ipsQuote")
		for idx, quote in enumerate(quotes):
			util.log(util.LogLevel.DEBUG, f"Processing quote [{idx}]")
			self._convert_quote(quote)
		return str(self.soup)

	def _convert_quote(self, blockquote: Tag, depth: int=0):
		if depth >= MAX_QUOTE_DEPTH:
			util.log(util.LogLevel.WARNING, f"Max quote depth ({MAX_QUOTE_DEPTH}) exceeded -- skipping further nesting.")
			return

		timestamp = blockquote.get("data-ipsquote-timestamp")
		username = blockquote.get("data-ipsquote-username")
		user_id = blockquote.get("data-ipsquote-userid")

		citation_div = blockquote.find("div", class_="ipsQuote_citation")
		if citation_div:
			# Toggle button
			a_toggle = citation_div.find("a", attrs={"data-action": "toggleQuote"})
			if a_toggle and "href" in a_toggle.attrs:
				a_toggle.attrs.pop("href", None)
			# Follow button
			a_right = citation_div.find("a", class_="ipsPos_right")
			if a_right and a_right.has_attr("href"):
				a_right["href"] = "https:" + a_right["href"]

		content_div = blockquote.find("div", class_="ipsQuote_contents")

		# Format timestamp
		date_str = "Unknown time"
		if timestamp:
			dt = datetime.fromtimestamp(int(timestamp))
			hour = dt.hour % 12 or 12
			minute = dt.minute
			ampm = "AM" if dt.hour < 12 else "PM"
			date_str = f"On {dt.month}/{dt.day}/{dt.year} at {hour}:{minute:02d} {ampm}"

		# Build new outer blockquote
		new_blockquote = self.soup.new_tag("blockquote", attrs={
			"class": "ipsQuote",
			"data-expanded": "false"
		})

		# Build citation line
		citation = self.soup.new_tag("div", attrs={"class": "ipsQuote_citation"})
		if username:
			user_link = self.soup.new_tag("a", href=self.ic.profile(user_id=user_id, username=username))
			user_link.string = username
			if a_toggle:
				citation.append(a_toggle)
			if a_right:
				citation.append(a_right)
			citation.append(f"{date_str}, ")
			citation.append(user_link)
			citation.append(" said:")
		else:
			citation.string = f"Quote"
		new_blockquote.append(citation)

		# Build content block
		contents = self.soup.new_tag("div", attrs={"class": "ipsQuote_contents"})
		if content_div:
			for child in content_div.contents:
				# If it's a nested ipsQuote blockquote, convert recursively
				if isinstance(child, type(self.soup.new_tag("div"))) and child.name == "blockquote":
					if "ipsQuote" in child.get("class", []):
						#util.log(util.LogLevel.DEBUG, "Nested ipsQuote found, converting recursively.")
						#nested_html = ContentConverter(str(child), self.ic, depth + 1).convert_quotes()
						#nested_soup = BeautifulSoup(nested_html, "html.parser")
						#for i, elem in enumerate(nested_soup.contents):
							#util.log(util.LogLevel.DEBUG, f"Appending nested element [{i}] at depth {self.depth + 1}")
							#contents.append(elem)
						util.log(util.LogLevel.DEBUG, f"Converting nested quote [depth={depth+1}]")
						self._convert_quote(child, depth=depth+1)
						contents.append(child)
						continue

				# Otherwise, append child as-is (preserve formatting)
				fragment = BeautifulSoup(str(child), "html.parser")
				for elem in fragment.contents:
					contents.append(elem)

		new_blockquote.append(contents)

		# Append expand label
		expand_label = self.soup.new_tag("div", attrs={"class": "expand-label"})
		expand_label.string = "Expand ▼"
		new_blockquote.append(expand_label)

		# Replace the original quote with the processed one
		blockquote.replace_with(new_blockquote)

	def convert_relative_resources(self):
		"""
		Convert relative resources (like images) to absolute URLs.
		"""
		imgs = self.soup.find_all("img")
		for img in imgs:
			src = img.get("src", "")
			if src.startswith("//"):
				# Protocol-relative (e.g. //example.com/image.png)
				img["src"] = "https:" + src
		
