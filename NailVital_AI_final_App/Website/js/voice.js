let recognition = null;
let isListening = false;
let synth = window.speechSynthesis;
let userExplicitlyStopped = true;
let aiIsSpeaking = false;

function initSpeechRecognition() {
    window.SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!window.SpeechRecognition) {
        console.warn("Speech Recognition API not supported in this browser.");
        return false;
    }
    
    recognition = new window.SpeechRecognition();
    recognition.continuous = true;
    recognition.interimResults = false;
    recognition.lang = 'en-US';

    recognition.onstart = () => {
        isListening = true;
        const fab = document.getElementById('micBtn');
        if (fab) fab.classList.add('listening');
        showVoiceToast("Listening...");
    };

    recognition.onresult = (event) => {
        const lastResultIndex = event.results.length - 1;
        const transcript = event.results[lastResultIndex][0].transcript.trim();
        if (!transcript) return;
        showVoiceToast(`You: "${transcript}"<br>Thinking...`);
        sendVoiceCommand(transcript);
    };

    recognition.onerror = (event) => {
        console.error("Speech recognition error:", event.error);
        showVoiceToast(`Error: ${event.error}`, 3000);
        stopListening();
    };

    recognition.onend = () => {
        if (!userExplicitlyStopped && !aiIsSpeaking) {
            try { recognition.start(); } catch(e) {}
        } else if (userExplicitlyStopped) {
            stopListening();
        }
    };

    return true;
}

function toggleVoiceAssistant() {
    if (!recognition && !initSpeechRecognition()) {
        toast("Voice Assistant is not supported in your browser.", "error");
        return;
    }

    if (isListening) {
        userExplicitlyStopped = true;
        stopListening(); // Instantly update UI
        try { recognition.stop(); } catch(e) {}
    } else {
        userExplicitlyStopped = false;
        // Stop any ongoing speech synthesis before listening again
        if (synth.speaking) {
            synth.cancel();
        }
        try { recognition.start(); } catch(e) {}
    }
}

function stopListening() {
    isListening = false;
    const fab = document.getElementById('micBtn');
    if (fab) fab.classList.remove('listening');
}

function showVoiceToast(htmlContent, duration = null) {
    const toastEl = document.getElementById('voiceStatusToast');
    if (!toastEl) return;
    toastEl.innerHTML = htmlContent;
    toastEl.classList.add('show');
    
    if (duration) {
        setTimeout(() => hideVoiceToast(), duration);
    }
}

function hideVoiceToast() {
    const toastEl = document.getElementById('voiceStatusToast');
    if (toastEl) {
        toastEl.classList.remove('show');
    }
}

async function sendVoiceCommand(transcript) {
    const token = localStorage.getItem(CONFIG.TOKEN_KEY);
    if (!token) {
        showVoiceToast("Please log in to use the Voice Assistant.", 4000);
        return;
    }

    try {
        const response = await fetch(`${CONFIG.API_BASE_URL}/voice-command`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ message: transcript })
        });

        if (!response.ok) {
            throw new Error(`HTTP Error ${response.status}`);
        }

        const data = await response.json();
        
        let responseHTML = ``;
        if (data.message) {
             responseHTML = `AI: "${data.message}"`;
             speakText(data.message);
        } else {
             responseHTML = `Action completed.`;
        }
        
        showVoiceToast(responseHTML, 5000);
        executeVoiceAction(data);

    } catch (error) {
        console.error("Voice Command Error:", error);
        showVoiceToast("Sorry, I couldn't process that right now.", 4000);
    }
}

function speakText(text) {
    if (!text || !synth) return;
    if (synth.speaking) {
        synth.cancel();
    }
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = 'en-US';
    
    utterance.onstart = () => {
        aiIsSpeaking = true;
        if (isListening && !userExplicitlyStopped) {
            recognition.stop();
        }
    };
    
    utterance.onend = () => {
        aiIsSpeaking = false;
        if (!userExplicitlyStopped) {
            try { recognition.start(); } catch(e) {}
        }
    };
    
    synth.speak(utterance);
}

function executeVoiceAction(data) {
    const actionType = data.action_type;
    
    if (actionType === 'NAVIGATE' && data.target) {
        handleNavigation(data.target);
    } 
    else if (actionType === 'ACTION' && data.target) {
        handleAction(data.target);
    }
    else if (actionType === 'MULTI' && data.commands) {
        data.commands.forEach((cmd, index) => {
            // Add slight delay between actions if needed, though sequential usually works
            setTimeout(() => {
                if (cmd.type === 'NAVIGATE') handleNavigation(cmd.target);
                if (cmd.type === 'ACTION') handleAction(cmd.target);
            }, index * 300);
        });
    }
}

function handleNavigation(target) {
    // Map backend targets to frontend screen IDs if they differ
    const targetMap = {
        'home': 'home',
        'scan': 'scan',
        'history': 'history',
        'chat': 'chatbot',
        'chatbot': 'chatbot',
        'profile': 'profile',
        'settings': 'profile',
        'health_wiki': 'wiki',
        'wiki': 'wiki',
        'login': 'login',
        'register': 'register',
        'about': 'splash'
    };
    
    const screenId = targetMap[target.toLowerCase()] || target;
    
    // Check if user is logged in for protected screens
    const token = localStorage.getItem(CONFIG.TOKEN_KEY);
    const protectedScreens = ['home', 'scan', 'history', 'chatbot', 'profile', 'wiki'];
    
    if (protectedScreens.includes(screenId) && !token) {
        toast("You must log in to access this screen.", "error");
        return;
    }
    
    if (typeof showScreen === 'function') {
        showScreen(screenId);
    }
}

function handleAction(target) {
    const action = target.toLowerCase();
    
    function scrollCurrentView(amount) {
        const chatMessages = document.querySelector('#chatbot.active .chat-messages');
        if (chatMessages) {
            chatMessages.scrollBy({ top: amount, behavior: 'smooth' });
            return;
        }
        
        const openModal = document.querySelector('.modal-overlay.active .modal-sheet');
        if (openModal) {
            openModal.scrollBy({ top: amount, behavior: 'smooth' });
            return;
        }

        const activeScreen = document.querySelector('.screen.active');
        if (activeScreen) {
            activeScreen.scrollBy({ top: amount, behavior: 'smooth' });
        } else {
            window.scrollBy({ top: amount, behavior: 'smooth' });
        }
    }
    
    if (action === 'scroll_down') {
        scrollCurrentView(500);
    }
    else if (action === 'scroll_up') {
        scrollCurrentView(-500);
    }
    else if (action === 'take_photo') {
        handleNavigation('scan');
        setTimeout(() => {
            const fileInput = document.getElementById('imageInput');
            if (fileInput) fileInput.click();
        }, 500);
    }
    else if (action.startsWith('open_disease:')) {
        // e.g. open_disease:aloperia_areata
        const disease = action.split(':')[1];
        if (typeof showWikiModal === 'function' && CONFIG.DISEASE_DETAILS[disease]) {
            showWikiModal(CONFIG.DISEASE_DETAILS[disease]);
        }
    }
}

// Handle Dragging and Clicking for Mic Button
document.addEventListener('DOMContentLoaded', () => {
    const micBtn = document.getElementById('micBtn');
    if (!micBtn) return;
    
    let isDragging = false;
    let startX, startY, initialX, initialY;
    let lastTapTime = 0;
    
    micBtn.addEventListener('mousedown', dragStart);
    micBtn.addEventListener('touchstart', dragStart, {passive: false});
    
    function dragStart(e) {
        if (e.type === 'touchstart') {
            startX = e.touches[0].clientX;
            startY = e.touches[0].clientY;
        } else {
            startX = e.clientX;
            startY = e.clientY;
        }
        
        const rect = micBtn.getBoundingClientRect();
        initialX = rect.left;
        initialY = rect.top;
        isDragging = false;
        
        document.addEventListener('mousemove', dragMove);
        document.addEventListener('mouseup', dragEnd);
        document.addEventListener('touchmove', dragMove, {passive: false});
        document.addEventListener('touchend', dragEnd);
    }
    
    function dragMove(e) {
        let currentX, currentY;
        if (e.type === 'touchmove') {
            currentX = e.touches[0].clientX;
            currentY = e.touches[0].clientY;
        } else {
            currentX = e.clientX;
            currentY = e.clientY;
        }
        
        const dx = currentX - startX;
        const dy = currentY - startY;
        
        if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
            isDragging = true;
        }
        
        if (isDragging) {
            e.preventDefault();
            let newX = initialX + dx;
            let newY = initialY + dy;
            
            // Constrain to window bounds
            newX = Math.max(0, Math.min(newX, window.innerWidth - micBtn.offsetWidth));
            newY = Math.max(0, Math.min(newY, window.innerHeight - micBtn.offsetHeight));
            
            micBtn.style.left = `${newX}px`;
            micBtn.style.top = `${newY}px`;
            micBtn.style.bottom = 'auto';
            micBtn.style.right = 'auto';
        }
    }
    
    function dragEnd(e) {
        document.removeEventListener('mousemove', dragMove);
        document.removeEventListener('mouseup', dragEnd);
        document.removeEventListener('touchmove', dragMove);
        document.removeEventListener('touchend', dragEnd);
        
        if (!isDragging) {
            const now = Date.now();
            if (now - lastTapTime > 300) { // Debounce to prevent touch + mouse double click
                toggleVoiceAssistant();
                lastTapTime = now;
            }
            if (e.cancelable) {
                e.preventDefault();
            }
        }
    }
});
