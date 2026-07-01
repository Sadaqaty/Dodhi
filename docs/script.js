document.addEventListener("DOMContentLoaded", () => {
    // 1. Hero Mockup Float Animation (CSS/JS hybrid)
    const heroMockup = document.querySelector('.hero-mockup-card');
    if (heroMockup) {
        let angle = 0;
        function floatHero() {
            angle += 0.015;
            const yOffset = Math.sin(angle) * 10;
            const rOffset = Math.sin(angle) * 0.4;
            heroMockup.style.transform = `rotateY(-10deg) rotateX(10deg) rotateZ(${-2 + rOffset}deg) translateY(${yOffset}px)`;
            requestAnimationFrame(floatHero);
        }
        floatHero();
    }

    // 2. Progressive Reveal on Scroll (Intersection Observer)
    const elementsToReveal = [
        ...document.querySelectorAll('.f-card'),
        ...document.querySelectorAll('.product-details .text'),
        ...document.querySelectorAll('.product-details .visual')
    ];

    if ('IntersectionObserver' in window) {
        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    entry.target.classList.add('revealed');
                    observer.unobserve(entry.target);
                }
            });
        }, {
            threshold: 0.1,
            rootMargin: '0px 0px -40px 0px'
        });

        elementsToReveal.forEach(el => {
            el.classList.add('reveal-on-scroll');
            observer.observe(el);
        });
    } else {
        // Fallback for older browsers
        elementsToReveal.forEach(el => {
            el.style.opacity = '1';
            el.style.transform = 'none';
        });
    }

    // 3. Mobile Navigation Menu Toggle
    const navToggle = document.querySelector('.nav-toggle');
    const navLinks = document.querySelector('.nav-links');

    if (navToggle && navLinks) {
        navToggle.addEventListener('click', (e) => {
            e.stopPropagation();
            navToggle.classList.toggle('active');
            navLinks.classList.toggle('active');
        });

        // Close menu on click outside
        document.addEventListener('click', (e) => {
            if (navLinks.classList.contains('active') && !navLinks.contains(e.target) && e.target !== navToggle) {
                navToggle.classList.remove('active');
                navLinks.classList.remove('active');
            }
        });
    }
});