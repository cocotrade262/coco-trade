import { Component } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
import { getTabsRoutePath } from './utils/tab-route.util';

@Component({
  selector: 'app-root',
  templateUrl: 'app.component.html',
  styleUrls: ['app.component.scss'],
  standalone: false,
})
export class AppComponent {
  constructor(private readonly router: Router) {
    if (typeof document === 'undefined') return;

    this.router.events.pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd)).subscribe(() => this.onTabsRouteTick());
    queueMicrotask(() => this.onTabsRouteTick());
    window.addEventListener('hashchange', () => this.onTabsRouteTick());
  }

  private onTabsRouteTick(): void {
    const path = getTabsRoutePath(this.router.url);
    let tab: 'feed' | 'post' | 'account' = 'feed';
    if (/^\/tabs\/post(\/|$)/.test(path)) tab = 'post';
    else if (/^\/tabs\/account(\/|$)/.test(path)) tab = 'account';
    document.body.dataset['activeTab'] = tab;

    const run = () => this.syncTabIonPageLayers(tab);
    run();
    requestAnimationFrame(() => requestAnimationFrame(run));
    setTimeout(run, 0);
    setTimeout(run, 50);
    setTimeout(run, 150);
    setTimeout(run, 400);
  }

  /** All ion-page nodes including inside ShadowRoots (tabs outlet often sits under #shadow-root). */
  private collectIonPagesDeep(): HTMLElement[] {
    const out: HTMLElement[] = [];
    const visit = (root: Document | ShadowRoot) => {
      try {
        root.querySelectorAll('ion-page').forEach((p) => out.push(p as HTMLElement));
        root.querySelectorAll('*').forEach((el) => {
          if (el.shadowRoot) visit(el.shadowRoot);
        });
      } catch {
        /* ignore */
      }
    };
    visit(document);
    return out;
  }

  private queryDeep(root: Document | ShadowRoot | Element, selector: string): Element | null {
    try {
      const hit = root.querySelector(selector);
      if (hit) return hit;
      const all = root.querySelectorAll('*');
      for (let i = 0; i < all.length; i++) {
        const el = all[i];
        if (el.shadowRoot) {
          const inner = this.queryDeep(el.shadowRoot, selector);
          if (inner) return inner;
        }
      }
    } catch {
      /* ignore */
    }
    return null;
  }

  private tabRoleForPage(page: HTMLElement): 'feed' | 'post' | 'account' | null {
    if (this.queryDeep(page, 'app-video-feed')) return 'feed';
    if (this.queryDeep(page, 'app-post-ad')) return 'post';
    if (this.queryDeep(page, 'app-account')) return 'account';
    return null;
  }

  /**
   * Show only the ion-page for the active tab; hide all other tab shells (full-viewport blockers).
   */
  private syncTabIonPageLayers(active: 'feed' | 'post' | 'account'): void {
    const pages = this.collectIonPagesDeep();
    for (const page of pages) {
      const role = this.tabRoleForPage(page);
      if (!role) continue;

      if (role === active) {
        /*
         * global.scss targets ion-page.ion-page-hidden { pointer-events: none !important }.
         * Ionic can leave the visible tab incorrectly flagged hidden — strip + force hit-testing.
         */
        page.classList.remove('ion-page-hidden', 'ion-page-invisible');
        page.style.removeProperty('display');
        page.style.setProperty('pointer-events', 'auto', 'important');
        page.style.setProperty('visibility', 'visible', 'important');
        page.style.position = 'relative';
        page.style.zIndex = '2147483646';
      } else {
        page.style.setProperty('display', 'none', 'important');
        page.style.setProperty('pointer-events', 'none', 'important');
        page.style.setProperty('visibility', 'hidden', 'important');
        page.style.zIndex = '0';
      }
    }
  }
}
