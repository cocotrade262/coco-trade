import { Component, OnDestroy } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { defer, EMPTY, filter, fromEvent, merge, of, Subscription } from 'rxjs';
import { getTabsRoutePath } from '../../utils/tab-route.util';

export type TabSegment = 'feed' | 'post' | 'account';

@Component({
  selector: 'app-tabs',
  templateUrl: './tabs.page.html',
  styleUrls: ['./tabs.page.scss'],
  standalone: false,
})
export class TabsPage implements OnDestroy {
  /** Only one tab body is mounted at a time — avoids stacked ion-page layers on Android. */
  activeTab: TabSegment = 'feed';

  private urlSub?: Subscription;

  constructor(private readonly router: Router) {
    this.syncActiveTab();
    this.urlSub = merge(
      defer(() => of(undefined)),
      this.router.events.pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd)),
      typeof window !== 'undefined' ? fromEvent(window, 'hashchange') : EMPTY
    ).subscribe(() => this.syncActiveTab());
  }

  ngOnDestroy(): void {
    this.urlSub?.unsubscribe();
  }

  private syncActiveTab(): void {
    const path = getTabsRoutePath(this.router.url);
    const m = /^\/tabs\/(feed|post|account)(\/|$)/.exec(path);
    const seg = m?.[1];
    if (seg === 'feed' || seg === 'post' || seg === 'account') {
      this.activeTab = seg;
      return;
    }
    this.activeTab = 'feed';
  }
}
