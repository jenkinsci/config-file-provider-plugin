document.addEventListener("DOMContentLoaded", function () {
    const textarea = document.getElementById("config.content");
    const contentTypeElement = document.querySelector("span[data-content-type]");

    if (textarea && contentTypeElement) {
        const contentType = contentTypeElement.dataset.contentType;
        const isReadOnly = contentTypeElement.hasAttribute("data-read-only");

        CodeMirror.fromTextArea(textarea, {
            lineNumbers: true,
            matchBrackets: true,
            mode: contentType,
            readOnly: isReadOnly,
        });
    }
});
