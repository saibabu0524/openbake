// ---------- Countdown ----------
// Set your launch date here (local time)
const LAUNCH_DATE = new Date('2026-08-01T09:00:00');

const els = {
    days: document.getElementById('days'),
    hours: document.getElementById('hours'),
    minutes: document.getElementById('minutes'),
    seconds: document.getElementById('seconds'),
};

function pad(n) {
    return String(n).padStart(2, '0');
}

function updateCountdown() {
    const diff = LAUNCH_DATE - new Date();

    if (diff <= 0) {
        els.days.textContent = '00';
        els.hours.textContent = '00';
        els.minutes.textContent = '00';
        els.seconds.textContent = '00';
        clearInterval(timer);
        return;
    }

    els.days.textContent = pad(Math.floor(diff / 86400000));
    els.hours.textContent = pad(Math.floor(diff / 3600000) % 24);
    els.minutes.textContent = pad(Math.floor(diff / 60000) % 60);
    els.seconds.textContent = pad(Math.floor(diff / 1000) % 60);
}

updateCountdown();
const timer = setInterval(updateCountdown, 1000);

// ---------- Notify form ----------
const form = document.getElementById('notify-form');
const emailInput = document.getElementById('email');
const button = document.getElementById('notify-btn');
const message = document.getElementById('form-message');

function showMessage(text, type) {
    message.textContent = text;
    message.className = type;
}

form.addEventListener('submit', async (event) => {
    event.preventDefault();

    const email = emailInput.value.trim();
    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        showMessage('Please enter a valid email address.', 'error');
        return;
    }

    button.disabled = true;
    showMessage('Submitting…', '');

    try {
        // Relative path so it works at any Tomcat context path (ROOT or /coming-soon)
        const response = await fetch('api/subscribe', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email }),
        });

        const data = await response.json().catch(() => ({}));

        if (response.ok) {
            showMessage(data.message || 'Thank you! We will notify you when we launch.', 'success');
            form.reset();
        } else {
            showMessage(data.message || 'Something went wrong. Please try again.', 'error');
        }
    } catch {
        showMessage('Could not reach the server. Please try again later.', 'error');
    } finally {
        button.disabled = false;
    }
});
