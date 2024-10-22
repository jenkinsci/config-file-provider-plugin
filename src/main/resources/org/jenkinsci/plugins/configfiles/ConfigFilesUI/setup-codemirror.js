document.addEventListener("DOMContentLoaded", function() {
    const textarea = document.getElementById("config.content");
    const contentTypeElement = document.querySelector(".content-type-config");

    const contentType = contentTypeElement.dataset.contentType;
    const readOnly = contentTypeElement.dataset.readOnly === "true";

    CodeMirror.fromTextArea(textarea, {
        lineNumbers: true,
        matchBrackets: true,
        mode: contentType,
        readOnly: readOnly,
    });
});
