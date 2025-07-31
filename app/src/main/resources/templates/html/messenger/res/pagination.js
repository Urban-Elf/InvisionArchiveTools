document.addEventListener("DOMContentLoaded", function () {
    const postContainer = document.querySelector("#post-container");
    const containers = Array.from(document.querySelectorAll("div.pagination-controls"));

    let totalPages = 1;
    let manifestPage = 1;

    // Load manifest
    function loadManifestFromTag() {
        const el = document.getElementById("manifest-json");
        if (!el) {
            console.error("Manifest script tag not found");
            return null;
        }
        try {
            return JSON.parse(el.textContent);
        } catch (err) {
            console.error("Error parsing manifest JSON:", err);
            return null;
        }
    }
    
    const manifest = loadManifestFromTag();
    
    if (manifest) {
        console.log("Loaded manifest:", manifest);
        totalPages = manifest.totalPages;
        manifestPage = manifest.page;
    }

    if (!postContainer || containers.length === 0) {
        console.warn("Missing posts container, page iframe, or pagination controls.");
        return;
    }

    let currentPage = manifestPage;
    let windowSize = 5;

    function showPage(page, initial=false) {
        if (initial) {
            if (totalPages > 1) {
                renderControls();
            } else {
                containers.forEach(container => {
                    container.style.marginTop = 0;
                    container.style.marginBottom = 0;
                });
            }
            return;
        }

        currentPage = page;
        renderControls();

        const nextPage = `${page}.html`;
        
        const viewportHeight = window.innerHeight;
        let fullHeight = document.documentElement.scrollHeight;
        const threshold = 120;
        const scrollY = window.scrollY;
        const isNearBottom = scrollY + viewportHeight >= fullHeight - threshold;

        if (!initial && isNearBottom) {
            window.scrollTo({ top: document.documentElement.scrollHeight, behavior: 'auto' });

            setTimeout(() => {
                window.scrollTo({ top: 0, behavior: 'smooth' });
            }, 50);

            window.addEventListener('scroll', () => {
                if (window.scrollY === 0) {
                    window.location.href = nextPage;
                }
            });
        } else {
            window.location.href = nextPage;
        }
	}

	function renderControls() {
	    containers.forEach(container => {
		    container.innerHTML = '';
        });

        if (currentPage == 1 || currentPage == totalPages) {
            windowSize = 5;
        } else {
            windowSize = 7;
        }

		// First page <<
		if (currentPage > 1) {
			containers.forEach(container => {
                const firstBtn = document.createElement("button");
                firstBtn.innerHTML = '<i style="font-size:12px;" class="fa fa-angle-double-left"></i>';
                firstBtn.onclick = () => {
                    showPage(1);
                };
			    container.appendChild(firstBtn);
            });
		}

		// Previous
		if (currentPage > 1) {
			containers.forEach(container => {
                const prevBtn = document.createElement("button");
                prevBtn.textContent = "PREV";
                let prevPage = currentPage - 1
                prevBtn.onclick = () => {
                    showPage(prevPage);
                };
			    container.appendChild(prevBtn);
            });
		}

		// Calculate window range
		let start = Math.max(1, currentPage - Math.floor(windowSize / 2));
		let end = Math.min(totalPages, start + windowSize - 1);

		// Adjust start if we're near the end
		if (end - start + 1 < windowSize) {
			start = Math.max(1, end - windowSize + 1);
		}

		for (let i = start; i <= end; i++) {
			containers.forEach(container => {
                const btn = document.createElement("button");
                btn.textContent = i;
                btn.disabled = (i === currentPage);
                btn.onclick = () => {
                    showPage(i);
                };
			    container.appendChild(btn);
            });
		}

		// Next
		if (currentPage < totalPages) {
			containers.forEach(container => {
                const nextBtn = document.createElement("button");
                nextBtn.textContent = "NEXT";
                let nextPage = currentPage + 1
                nextBtn.onclick = () => {
                    showPage(nextPage);
                };
			    container.appendChild(nextBtn);
            });
		}

		// Last >>
		if (currentPage < totalPages) {
			containers.forEach(container => {
                const lastBtn = document.createElement("button");
                lastBtn.innerHTML = '<i style="font-size:12px;" class="fa fa-angle-double-right"></i>';
                lastBtn.onclick = () => {
                    showPage(totalPages);
                };
			    container.appendChild(lastBtn);
            });
		}
		
        // Manual Page Input: "Page X of Y"
        containers.forEach(container => {
            const wrapper = document.createElement("div");
            wrapper.style.display = "inline-block";
            wrapper.style.position = "relative";
            wrapper.style.marginLeft = "0.5em";

            const toggle = document.createElement("button");
            toggle.textContent = `Page ${currentPage} of ${totalPages} ▾`;
            toggle.className = "page-dropdown-toggle";

            const popup = document.createElement("div");
            popup.className = "page-dropdown-popup";

            // Triangle "tail"
            const tail = document.createElement("div");
            tail.className = "popup-tail";
            popup.appendChild(tail);

            const input = document.createElement("input");
            input.type = "number";
            input.placeholder = "Page number";
            input.min = 1;
            input.max = totalPages;
            input.className = "page-input";

            const errLabel = document.createElement("p");
            errLabel.className = "page-error-label";
            errLabel.style.display = "none"; // Initially hidden

            const goBtn = document.createElement("button");
            goBtn.textContent = "Go";
            goBtn.className = "page-go-button";
            goBtn.onclick = () => {
                const target = parseInt(input.value);
                if (isNaN(target)) {
                    errLabel.innerText = "Please input a number.";
                    errLabel.style.display = "block";
                    return;
                } else if (target < 1) {
                    errLabel.innerText = "Number must be >= 1.";
                    errLabel.style.display = "block";
                    return;
                } else if (target > totalPages) {
                    errLabel.innerText = `Number must be <= ${totalPages}.`;
                    errLabel.style.display = "block";
                    return;
                }
                errLabel.style.display = "none"; // Hide error label
                showPage(target);
                popup.style.display = "none";
            };

            popup.appendChild(input);
            popup.appendChild(errLabel);
            popup.appendChild(goBtn);
            wrapper.appendChild(toggle);
            wrapper.appendChild(popup);
            container.appendChild(wrapper);

            toggle.onclick = (e) => {
                e.stopPropagation();
                popup.style.display = (popup.style.display === "block") ? "none" : "block";
            };

            // Click-away to close
            document.addEventListener("click", (e) => {
                if (!wrapper.contains(e.target) && e.target != container) {
                    popup.style.display = "none";
                }
            });
        });
	}
    // ... rest of your functions with fetch corrected to `/pages/page-${page}.html`

    // Load saved page or default
    /*const savedPage = localStorage.getItem('currentPage');
    if (savedPage) {
        let page = parseInt(savedPage);
        if (page < 1 || page > totalPages) {
            showPage(1, false);
        } else {
            showPage(page, false);
        }
    } else {
        showPage(1, false);
    }*/
    showPage(manifestPage, true);
});


/*document.addEventListener("DOMContentLoaded", function () {
	const postContainer = Array.from(document.querySelector("#post-container"));
	const containers = Array.from(document.querySelectorAll("div.pagination-controls"));
	
	console.log("Loaded", { posts, containers });

	if (posts.length === 0 || containers.length === 0) {
		console.warn("Missing posts or containers.");
		return;
	}

	const totalPages = Math.max(...posts.map(p => parseInt(p.dataset.page)));
	let currentPage = 1;
	const windowSize = 5;

	function showPage(page, scrollToTop=true) {
        const viewportHeight = window.innerHeight;
        let fullHeight = document.documentElement.scrollHeight;
        const threshold = 120;
        const scrollY = window.scrollY;
        const isNearBottom = scrollY + viewportHeight >= fullHeight - threshold;

		currentPage = page;
        localStorage.setItem('currentPage', currentPage);

        postContainer.innerHTML = "<p>Loading...</p>";

        fetch(`/pages/${pageNumber-1}.html`)
            .then(res => {
                if (!res.ok) throw new Error("Page load failed");
                return res.text();
            })
            .then(html => {
                postContainer.innerHTML = html;
            })
            .catch(err => {
                postContainer.innerHTML = "<p>Failed to load page.</p>";
                console.error(err);
            });

		/*posts.forEach(p => {
			p.style.display = (parseInt(p.dataset.page) === page) ? "block" : "none";
		});/

        if (totalPages > 1) {
            renderControls();
        } else {
            containers.forEach(container => {
                container.style.marginTop = 0;
                container.style.marginBottom = 0;
            });
        }

        if (scrollToTop && isNearBottom) {
            //console.log("Near bottom, scrolling to top...");
            window.scrollTo({ top: document.documentElement.scrollHeight, behavior: 'auto' });

            setTimeout(() => {
                window.scrollTo({ top: 0, behavior: 'smooth' });
            }, 50);
        }
	}

	function renderControls() {
	    containers.forEach(container => {
		    container.innerHTML = '';
        });

		// First page <<
		if (currentPage > 1) {
			containers.forEach(container => {
                const firstBtn = document.createElement("button");
                firstBtn.innerHTML = '<i style="font-size:12px;" class="fa fa-angle-double-left"></i>';
                firstBtn.onclick = () => {
                    showPage(1);
                };
			    container.appendChild(firstBtn);
            });
		}

		// Previous
		if (currentPage > 1) {
			containers.forEach(container => {
                const prevBtn = document.createElement("button");
                prevBtn.textContent = "PREV";
                let prevPage = currentPage - 1
                prevBtn.onclick = () => {
                    showPage(prevPage);
                };
			    container.appendChild(prevBtn);
            });
		}

		// Calculate window range
		let start = Math.max(1, currentPage - Math.floor(windowSize / 2));
		let end = Math.min(totalPages, start + windowSize - 1);

		// Adjust start if we're near the end
		if (end - start + 1 < windowSize) {
			start = Math.max(1, end - windowSize + 1);
		}

		for (let i = start; i <= end; i++) {
			containers.forEach(container => {
                const btn = document.createElement("button");
                btn.textContent = i;
                btn.disabled = (i === currentPage);
                btn.onclick = () => {
                    showPage(i);
                };
			    container.appendChild(btn);
            });
		}

		// Next
		if (currentPage < totalPages) {
			containers.forEach(container => {
                const nextBtn = document.createElement("button");
                nextBtn.textContent = "NEXT";
                let nextPage = currentPage + 1
                nextBtn.onclick = () => {
                    showPage(nextPage);
                };
			    container.appendChild(nextBtn);
            });
		}

		// Last >>
		if (currentPage < totalPages) {
			containers.forEach(container => {
                const lastBtn = document.createElement("button");
                lastBtn.innerHTML = '<i style="font-size:12px;" class="fa fa-angle-double-right"></i>';
                lastBtn.onclick = () => {
                    showPage(totalPages);
                };
			    container.appendChild(lastBtn);
            });
		}
		
        // Manual Page Input: "Page X of Y"
        containers.forEach(container => {
            const wrapper = document.createElement("div");
            wrapper.style.display = "inline-block";
            wrapper.style.position = "relative";
            wrapper.style.marginLeft = "0.5em";

            const toggle = document.createElement("button");
            toggle.textContent = `Page ${currentPage} of ${totalPages} ▾`;
            toggle.className = "page-dropdown-toggle";

            const popup = document.createElement("div");
            popup.className = "page-dropdown-popup";

            // Triangle "tail"
            const tail = document.createElement("div");
            tail.className = "popup-tail";
            popup.appendChild(tail);

            const input = document.createElement("input");
            input.type = "number";
            input.placeholder = "Page number";
            input.min = 1;
            input.max = totalPages;
            input.className = "page-input";

            const errLabel = document.createElement("p");
            errLabel.className = "page-error-label";
            errLabel.style.display = "none"; // Initially hidden

            const goBtn = document.createElement("button");
            goBtn.textContent = "Go";
            goBtn.className = "page-go-button";
            goBtn.onclick = () => {
                const target = parseInt(input.value);
                if (isNaN(target)) {
                    errLabel.innerText = "Please input a number.";
                    errLabel.style.display = "block";
                    return;
                } else if (target < 1) {
                    errLabel.innerText = "Number must be >= 1.";
                    errLabel.style.display = "block";
                    return;
                } else if (target > totalPages) {
                    errLabel.innerText = `Number must be <= ${totalPages}.`;
                    errLabel.style.display = "block";
                    return;
                }
                errLabel.style.display = "none"; // Hide error label
                showPage(target);
                popup.style.display = "none";
            };

            popup.appendChild(input);
            popup.appendChild(errLabel);
            popup.appendChild(goBtn);
            wrapper.appendChild(toggle);
            wrapper.appendChild(popup);
            container.appendChild(wrapper);

            toggle.onclick = (e) => {
                e.stopPropagation();
                popup.style.display = (popup.style.display === "block") ? "none" : "block";
            };

            // Click-away to close
            document.addEventListener("click", (e) => {
                if (!wrapper.contains(e.target) && e.target != container) {
                    popup.style.display = "none";
                }
            });
        });
	}

    const savedPage = localStorage.getItem('currentPage');
	if (savedPage) {
        let page = parseInt(savedPage);
        console.log(page);
        if (page < 1 || page > totalPages) {
            showPage(1, false);
        } else {
            showPage(page, false);
        }
	} else {
	    showPage(1, false);
    }
});*/