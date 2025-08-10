Module.onRuntimeInitialized = () => {
    console.log("Skiko WASM runtime initialized!"); // Dobry punkt do debugowania

    // Teraz mamy pewność, że wszystkie funkcje typu ..._1nMake istnieją.
    // Ładujemy główny skrypt aplikacji.
    const appScript = document.createElement('script');
    appScript.src = 'extension-popup.js';
    document.body.appendChild(appScript);
};

// Dodatkowy "bezpiecznik" - na wypadek gdyby skrypt był już załadowany
// w momencie wykonywania init.js (mało prawdopodobne, ale możliwe)
if (Module.calledRun) {
    Module.onRuntimeInitialized();
}