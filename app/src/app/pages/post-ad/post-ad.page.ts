import { Component, HostBinding, OnDestroy } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { ToastController, ViewDidEnter, ViewWillEnter } from '@ionic/angular';
import { defer, EMPTY, filter, fromEvent, merge, of, Subscription } from 'rxjs';
import { PostsService } from '../../services/posts.service';
import { TabShellSyncService } from '../../services/tab-shell-sync.service';
import { getTabsRoutePath } from '../../utils/tab-route.util';

@Component({
  selector: 'app-post-ad',
  templateUrl: './post-ad.page.html',
  styleUrls: ['./post-ad.page.scss'],
  standalone: false,
})
export class PostAdPage implements OnDestroy, ViewWillEnter, ViewDidEnter {
  caption = '';
  selectedObjectUrl: string | null = null;
  durationSec: number | null = null;
  busy = false;

  /** Stack above other tab pages that stay mounted (full-viewport feed). */
  @HostBinding('style.z-index')
  hostZIndex = 1;

  /** Ensure this tab shell accepts taps (feed overlay uses pointer-events: none when inactive). */
  @HostBinding('style.pointer-events')
  hostPointerEvents = 'auto';

  private routeSub?: Subscription;

  constructor(
    private readonly postsService: PostsService,
    private readonly toastCtrl: ToastController,
    private readonly router: Router,
    private readonly tabShellSync: TabShellSyncService
  ) {
    this.routeSub = merge(
      defer(() => of(undefined)),
      this.router.events.pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd)),
      typeof window !== 'undefined' ? fromEvent(window, 'hashchange') : EMPTY
    ).subscribe(() => this.syncHostStack());
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
  }

  ionViewWillEnter(): void {
    this.syncHostStack();
  }

  ionViewDidEnter(): void {
    this.tabShellSync.scheduleSync();
  }

  private syncHostStack(): void {
    const path = getTabsRoutePath(this.router.url);
    const onPost = /^\/tabs\/post(\/|$)/.test(path);
    this.hostZIndex = onPost ? 50 : 1;
  }

  async onFileSelected(ev: Event) {
    if (this.busy) return;

    const input = ev.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    input.value = '';

    if (!file.type.startsWith('video/')) {
      await this.toast('Please select a video file.');
      return;
    }

    this.busy = true;
    try {
      if (this.selectedObjectUrl) URL.revokeObjectURL(this.selectedObjectUrl);

      const objectUrl = URL.createObjectURL(file);
      const duration = await this.getVideoDurationSec(objectUrl);

      if (!Number.isFinite(duration) || duration <= 0) {
        URL.revokeObjectURL(objectUrl);
        await this.toast('Could not read video duration. Try another file.');
        return;
      }

      if (duration < 30) {
        URL.revokeObjectURL(objectUrl);
        await this.toast('Minimum video duration is 30 seconds.');
        return;
      }

      this.selectedObjectUrl = objectUrl;
      this.durationSec = duration;
    } finally {
      this.busy = false;
    }
  }

  async publish() {
    if (!this.selectedObjectUrl || !this.durationSec) return;
    this.postsService.addVideoPost({ objectUrl: this.selectedObjectUrl, durationSec: this.durationSec });
    this.selectedObjectUrl = null;
    this.durationSec = null;
    this.caption = '';
    await this.toast('Posted to feed.');
  }

  private getVideoDurationSec(objectUrl: string): Promise<number> {
    return new Promise((resolve, reject) => {
      const v = document.createElement('video');
      v.preload = 'metadata';
      v.src = objectUrl;
      const cleanup = () => {
        v.onloadedmetadata = null;
        v.onerror = null;
      };
      v.onloadedmetadata = () => {
        cleanup();
        resolve(v.duration);
      };
      v.onerror = () => {
        cleanup();
        reject(new Error('Failed to load video metadata'));
      };
    });
  }

  private async toast(message: string) {
    const t = await this.toastCtrl.create({ message, duration: 1800, position: 'bottom' });
    await t.present();
  }
}
