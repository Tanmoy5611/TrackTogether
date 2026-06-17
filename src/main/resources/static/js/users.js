const input = document.getElementById("search");
const roleFilter = document.getElementById("roleFilter");
const table = document.getElementById("userTable");

let timeout = null;

if (input && roleFilter && table) {
    document
        .querySelectorAll("[data-user-href]")
        .forEach(attachUserRowClick);

    input.addEventListener("input", () => {
        clearTimeout(timeout);
        timeout = setTimeout(fetchUsers, 300);
    });

    roleFilter.addEventListener("change", fetchUsers);
}

function fetchUsers() {
    const name = input.value;
    const role = roleFilter.value;

    fetch(`/super_admin/api/user?name=${encodeURIComponent(name)}&role=${encodeURIComponent(role)}`)
        .then(res => res.json())
        .then(users => {
            table.replaceChildren();

            if (users.length === 0) {
                const row = document.createElement("tr");
                const cell = document.createElement("td");

                cell.colSpan = 4;
                cell.textContent = "No users found";

                row.appendChild(cell);
                table.appendChild(row);
                return;
            }

            users.forEach(user => {
                const row = document.createElement("tr");

                row.dataset.userHref = `/super_admin/user/${user.id}`;
                attachUserRowClick(row);

                appendTextCell(row, user.name);
                appendTextCell(row, user.email);
                appendTextCell(row, user.status);
                appendTextCell(row, user.role);

                table.appendChild(row);
            });
        });
}

function appendTextCell(row, value) {
    const cell = document.createElement("td");
    cell.textContent = value ?? "";
    row.appendChild(cell);
}

function attachUserRowClick(row) {
    row.style.cursor = "pointer";
    row.addEventListener("click", () => {
        window.location.href = row.dataset.userHref;
    });
}