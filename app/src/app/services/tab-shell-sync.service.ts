import { Injectable, NgZone } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
import { Capacitor } from '@capacitor/core';
import { App } from '@capacitor/app';
import { getTabsRoutePath } from '../utils/tab-route.util';

export type TabShellKey = 'feed' | 'post' | 'account';

/**
 * Ionic tabs keep inactive routes as stacked ion-pages in the router outlet (above the tab bar only).
 * Inactive Feed can cover Post touches on Android while bottom tab bar still works.
 */
@Injectable({ providedIn: 'root' })
export class TabShellSyncService {
  constructor(
    private readonly router: Router,
    private readonly zone: NgZone
  ) {
    if (typeof document === 'undefined') return;

    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(() => this.zone.run(() => this.scheduleSync()));

    queueMicrotask(() => this.zone.run(() => this.scheduleSync()));

    window.addEventListener('hashchange', () => this.zone.run(() => this.scheduleSync()));

    if (Capacitor.isNativePlatform()) {
      void App.addListener('resume', () => {
        this.zone.run(() => this.scheduleSync());
      });
    }

    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'visible') {
        this.zone.run(() => this.scheduleSync());
      }
    });
  }

  /** Public so tab pages can re-run after `ionViewDidEnter` (Android WebView timing). */
  scheduleSync(): void {
    const tab = this.activeTabFromUrl();
    document.body.dataset['activeTab'] = tab;

    const run = () => this.syncTabIonPageLayers(tab);
    run();
    requestAnimationFrame(() => requestAnimationFrame(() => this.zone.run(run)));
    for (const ms of [0, 16, 50, 100, 200, 400, 800, 1500, 2500]) {
      setTimeout(() => this.zone.run(run), ms);
    }
  }

  private activeTabFromUrl(): TabShellKey {
    const path = getTabsRoutePath(this.router.url);
    if (/^\/tabs\/post(\/|$)/.test(path)) return 'post';
    if (/^\/tabs\/account(\/|$)/.test(path)) return 'account';
    return 'feed';
  }

  private parentOrHost(el: Element | null): Element | null {
    if (!el) return null;
    if (el.parentElement) return el.parentElement;
    const r = el.getRootNode();
    if (r instanceof ShadowRoot && r.host) return r.host;
    return null;
  }

  private collectIonPagesDeep(): HTMLElement[] {
    const seen = new Set<HTMLElement>();
    const out: HTMLElement[] = [];

    const visit = (root: Document | ShadowRoot) => {
      try {
        root.querySelectorAll('ion-page').forEach((p) => {
          const el = p as HTMLElement;
          if (!seen.has(el)) {
            seen.add(el);
            out.push(el);
          }
        });
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

  private tabRoleForPage(page: HTMLElement): TabShellKey | null {
    if (this.queryDeep(page, 'app-video-feed')) return 'feed';
    if (this.queryDeep(page, 'app-post-ad')) return 'post';
    if (this.queryDeep(page, 'app-account')) return 'account';
    return null;
  }

  private syncTabIonPageLayers(active: TabShellKey): void {
    /* Direct pass: collapse ion-page above every Feed host when Post/Account is active. */
    if (active !== 'feed') {
      document.querySelectorAll('app-video-feed').forEach((host) => {
        let el: Element | null = host;
        while (el) {
          if (el.tagName === 'ION-PAGE') {
            const page = el as HTMLElement;
            page.style.setProperty('display', 'none', 'important');
            page.style.setProperty('pointer-events', 'none', 'important');
            page.style.setProperty('visibility', 'hidden', 'important');
            page.style.zIndex = '0';
            break;
          }
          el = this.parentOrHost(el);
        }
      });
    }

    const pages = this.collectIonPagesDeep();
    for (const page of pages) {
      const role = this.tabRoleForPage(page);
      if (!role) continue;

      if (role === active) {
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
