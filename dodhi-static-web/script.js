gsap.registerPlugin(ScrollTrigger, ScrollToPlugin);

gsap.set('.main', {position:'fixed', background:'#000', width:'100%', height:'100%', top:0, left:0})
gsap.set('.scrollDist', {width:'100%', height:'9500px'})

// Initial mask state
gsap.set('#m-cloud rect', {y: 799});

const heroTl = gsap.timeline({
    scrollTrigger: {
        trigger: '.scrollDist',
        start: 'top top',
        end: 'bottom bottom',
        scrub: 3 
    }
});

// 1. Cinematic Parallax & Mask Reveal
heroTl.fromTo('.sky', {y:0},{y:-150, ease: 'power2.inOut'}, 0)
  .fromTo('.mountBg', {y:0},{y:-100, ease: 'power2.inOut'}, 0)
  .fromTo('.cloud-mg', {y:100},{y:-300, ease: 'power2.inOut'}, 0)
  .fromTo('.mountMg', {y:0},{y:-250, ease: 'power2.inOut'}, 0)
  .fromTo('.cloud-fg-base', {y:50},{y:-500, ease: 'power2.inOut'}, 0)
  .fromTo('.mountFg', {y:0},{y:-600, ease: 'power2.inOut'}, 0)
  .fromTo('.cloud-top', {y:100},{y:-800, ease: 'power2.inOut'}, 0)
  .fromTo('.cloud-btm', {y:200},{y:-600, ease: 'power2.inOut'}, 0)
  .fromTo('.cloud-mid', {y:0},{y:-900, ease: 'power2.inOut'}, 0)
  .fromTo('#m-cloud rect', {y:799}, {y:0, ease: 'power3.inOut'}, 0)
  .to('.page-body', {top: '0%', duration: 0.45, ease: 'power3.inOut'}, 0.3)
  .to('.hero-parallax', {opacity: 0, duration: 0.25, ease: 'power2.inOut'}, 0.55);

// 2. Smoother Hero Text Transition
heroTl.to('.t-1', {opacity: 0, y: -120, duration: 0.15, ease: 'power4.inOut'}, 0.05)
  .to('.t-2', {opacity: 1, y: 0, duration: 0.15, ease: 'power4.inOut'}, 0.15)
  .to('.hero-tagline', {y: -80, opacity: 0, duration: 0.15, ease: 'power4.inOut'}, 0.1);

// 3. Robust Body Scroll (Optimized for all screen heights)
ScrollTrigger.create({
    trigger: ".scrollDist",
    start: "top top",
    end: "bottom bottom",
    onUpdate: (self) => {
        const startPoint = 0.32;
        if (self.progress > startPoint) {
            const contentScroll = (self.progress - startPoint) / (1 - startPoint);
            // Dynamic scroll range based on content height
            const bodyHeight = document.querySelector('.page-body').offsetHeight;
            const viewportHeight = window.innerHeight;
            const maxScroll = Math.max(bodyHeight - viewportHeight, 2000); // safety fallback
            
            gsap.set('.page-body', { y: -contentScroll * maxScroll });
            
            // REMOVED Black Fade Animation - Direct flow to About Fixare
        } else {
            gsap.set('.page-body', { y: 0 });
        }
    }
});

// Arrow Navigation (Cinematic Smooth Scroll)
document.querySelector('#arrowBtn').addEventListener('click', () => {
    gsap.to(window, {scrollTo: {y: window.innerHeight * 2.8}, duration: 2.2, ease: 'power4.inOut'});
});