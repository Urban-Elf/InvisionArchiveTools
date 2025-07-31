function formatTimeAgo(dateStr) {
	const now = new Date();
	const then = new Date(dateStr);
	const diffMs = now - then;
	const diffSec = Math.floor(diffMs / 1000);
	const diffMin = Math.floor(diffSec / 60);
	const diffHr = Math.floor(diffMin / 60);
	const diffDay = Math.floor(diffHr / 24);

	const optionsTime = { hour: 'numeric', minute: '2-digit', hour12: true };
	const optionsDate = { month: 'long', day: 'numeric', year: 'numeric' };

	if (diffSec < 40) return 'Just now';
	else if (diffMin < 60) return `${diffMin} minute${diffMin !== 1 ? 's' : ''} ago`;
	else if (
		diffHr < 24 &&
		now.toDateString() === then.toDateString()
	) {
		return `${diffHr} hour${diffHr !== 1 ? 's' : ''} ago`;
	}

	// Yesterday
	const yesterday = new Date();
	yesterday.setDate(now.getDate() - 1);
	if (yesterday.toDateString() === then.toDateString()) {
		return `Yesterday at ${then.toLocaleTimeString([], optionsTime)}`;
	}

	// Within the past week
	const diffDays = Math.floor((now - then) / (1000 * 60 * 60 * 24));
	if (diffDays < 7) {
		return `${then.toLocaleDateString(undefined, { weekday: 'long' })} at ${then.toLocaleTimeString([], optionsTime)}`;
	}

	// Fallback to full date
	const isCurrentYear = then.getFullYear() === new Date().getFullYear();
	const fallbackOptions = {
		month: 'long',
		day: 'numeric',
		...(isCurrentYear ? {} : { year: 'numeric' }) // include year only if not this year
	};
	return then.toLocaleDateString(undefined, fallbackOptions);
}

document.querySelectorAll('time.time-ago').forEach(timeEl => {
	const datetime = timeEl.getAttribute('datetime');
	const then = new Date(datetime);

	// Set relative label
	timeEl.textContent = formatTimeAgo(datetime);

	// Set title to "MM/DD/YYYY HH:MM AM/PM"
	const title = then.toLocaleString(undefined, {
		month: '2-digit',
		day: '2-digit',
		year: 'numeric',
		hour: 'numeric',
		minute: '2-digit',
		hour12: true
	});
	timeEl.title = title;
});
