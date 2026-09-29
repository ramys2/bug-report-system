import $ from "jquery";

/**
 * `GET /api/accounts/developers`: users that can be chosen as assignee.
 *
 * @returns {JQuery.jqXHR} resolves with a list of `{ id, name }`
 */
export function getDevelopers() {
    return $.ajax({
        method: "GET",
        url: "/api/accounts/developers"
    });
}

/**
 * `GET /api/projects`, reduced to the id and name needed for a select box.
 *
 * @returns {JQuery.Promise<Array<{id: string, name: string}>>}
 */
export function getProjects() {
    return $.ajax({
        method: "GET",
        url: "/api/projects"
    }).then(toOptions);
}

/**
 * `GET /api/components`, reduced to the id and name needed for a select box.
 *
 * @returns {JQuery.Promise<Array<{id: string, name: string}>>}
 */
export function getComponents() {
    return $.ajax({
        method: "GET",
        url: "/api/components"
    }).then(toOptions);
}

/**
 * Keeps only `id` and `name` of each item.
 */
function toOptions(items) {
    return items.map((item) => ({
        id: item.id,
        name: item.name
    }));
}
