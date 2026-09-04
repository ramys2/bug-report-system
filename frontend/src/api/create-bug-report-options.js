import $ from "jquery";

export function getDevelopers() {
    return $.ajax({
        method: "GET",
        url: "/api/accounts/developers"
    });
}

export function getProjects() {
    return $.ajax({
        method: "GET",
        url: "/api/projects"
    }).then(toOptions);
}

export function getComponents() {
    return $.ajax({
        method: "GET",
        url: "/api/components"
    }).then(toOptions);
}

function toOptions(items) {
    return items.map((item) => ({
        id: item.id,
        name: item.name
    }));
}
