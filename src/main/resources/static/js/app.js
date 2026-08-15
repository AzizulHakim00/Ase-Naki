const navButton = document.querySelector("[data-nav-toggle]");
const navigation = document.querySelector("[data-nav]");

if (navButton && navigation) {
    navButton.addEventListener("click", () => {
        navigation.classList.toggle("open");
    });
}

const utilitySelect = document.querySelector("[data-utility-select]");
const statusSelect = document.querySelector("[data-status-select]");

function filterStatuses() {
    if (!utilitySelect || !statusSelect) {
        return;
    }

    const option = utilitySelect.options[utilitySelect.selectedIndex];
    const allowed = (option.dataset.statuses || "").split(",");

    for (const statusOption of statusSelect.options) {
        if (!statusOption.dataset.status) {
            continue;
        }
        statusOption.hidden = allowed.length > 0
            && !allowed.includes(statusOption.dataset.status);
    }

    const selectedStatus = statusSelect.selectedOptions[0];
    if (selectedStatus && selectedStatus.hidden) {
        statusSelect.value = "";
    }
}

if (utilitySelect && statusSelect) {
    utilitySelect.addEventListener("change", filterStatuses);
    filterStatuses();
}

const fileInput = document.querySelector("[data-file-input]");
const fileLabel = document.querySelector("[data-file-label]");

if (fileInput && fileLabel) {
    fileInput.addEventListener("change", () => {
        fileLabel.textContent = fileInput.files.length
            ? fileInput.files[0].name
            : "JPG, PNG, WebP or PDF ? maximum 5 MB";
    });
}
