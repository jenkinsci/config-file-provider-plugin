document.addEventListener("DOMContentLoaded", function() {
    const textarea = document.querySelector("textarea#config\\.content");
    const contentTypeElement = document.querySelector(".content-type-config");

    const contentType = contentTypeElement.getAttribute("data-content-type");
    const readOnly = contentTypeElement.getAttribute("data-read-only") === "true";

    CodeMirror.fromTextArea(textarea, {
        lineNumbers: true,
        matchBrackets: true,
        mode: contentType,
        readOnly: readOnly,
    });
});
