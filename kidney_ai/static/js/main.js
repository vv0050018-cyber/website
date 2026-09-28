// KIDNEY AI - Frontend Utilities

function togglePasswordVisibility(fieldId, btn) {
    const input = document.getElementById(fieldId);
    if (!input) return;
    const isPw = input.type === 'password';
    input.type = isPw ? 'text' : 'password';
    const icon = btn.querySelector('i');
    if (icon) {
        icon.className = isPw ? 'fa-solid fa-eye-slash' : 'fa-solid fa-eye';
    }
}

function checkPasswordStrength(password) {
    const bar = document.getElementById('strengthBar');
    const text = document.getElementById('strengthText');
    if (!bar || !text) return;

    let score = 0;
    if (password.length >= 6) score++;
    if (password.length >= 10) score++;
    if (/[A-Z]/.test(password)) score++;
    if (/[0-9]/.test(password)) score++;
    if (/[^A-Za-z0-9]/.test(password)) score++;

    if (score === 0) {
        bar.style.width = '0%';
        bar.style.backgroundColor = '#E2E8F0';
        text.innerText = 'Password strength: None';
    } else if (score <= 2) {
        bar.style.width = '30%';
        bar.style.backgroundColor = '#EF4444';
        text.innerText = 'Password strength: Weak';
    } else if (score <= 4) {
        bar.style.width = '70%';
        bar.style.backgroundColor = '#F59E0B';
        text.innerText = 'Password strength: Moderate';
    } else {
        bar.style.width = '100%';
        bar.style.backgroundColor = '#10B981';
        text.innerText = 'Password strength: Strong (Clinical Grade)';
    }
}
