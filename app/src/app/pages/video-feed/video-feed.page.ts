import { AfterViewInit, Component, ElementRef, HostBinding, OnDestroy, ViewChildren } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { ModalController, ToastController, ViewWillEnter, ViewWillLeave } from '@ionic/angular';
import { filter, map, Subscription } from 'rxjs';
import { PostsService, PostVideo } from '../../services/posts.service';
import { ContactSheetComponent } from './contact-sheet.component';

@Component({
  selector: 'app-video-feed',
  templateUrl: './video-feed.page.html',
  styleUrls: ['./video-feed.page.scss'],
  standalone: false,
})
export class VideoFeedPage implements AfterViewInit, OnDestroy, ViewWillEnter, ViewWillLeave {
  /**
   * While another tab is active, this route can remain mounted (preload/tabs stack).
   * The 100vh reel layer must not capture taps meant for Post / Account.
   */
  feedTabInactive = true;

  /** Block the entire tab shell (`app-video-feed`), not only ion-content — lifecycle hooks alone are unreliable. */
  @HostBinding('style.pointer-events')
  get hostPointerEvents(): string {
    return this.feedTabInactive ? 'none' : 'auto';
  }

  readonly posts$ = this.postsService.posts$.pipe(
    map((posts) => (posts.length ? posts : this.getEmptyStatePosts()))
  );

  @ViewChildren('videoEl') videoEls!: ElementRef<HTMLVideoElement>[];

  private io?: IntersectionObserver;
  private sub?: Subscription;
  private routeSub?: Subscription;
  private latestPosts: PostVideo[] = [];

  constructor(
    private readonly postsService: PostsService,
    private readonly modalCtrl: ModalController,
    private readonly toastCtrl: ToastController,
    private readonly router: Router
  ) {
    this.syncFeedInactiveFromUrl(this.router.url);
    this.routeSub = this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(() => this.syncFeedInactiveFromUrl(this.router.url));
  }

  ionViewWillEnter(): void {
    this.syncFeedInactiveFromUrl(this.router.url);
  }

  ionViewWillLeave(): void {
    this.feedTabInactive = true;
  }

  private syncFeedInactiveFromUrl(url: string): void {
    const path = this.pathAfterHash(url);
    // Inactive unless the active tab is really "feed" (hash or path routing).
    const onFeedTab = /\/tabs\/feed(\/|$|\?)/.test(path) || path.endsWith('/tabs/feed');
    this.feedTabInactive = !onFeedTab;
  }

  private pathAfterHash(url: string): string {
    const noQuery = url.split('?')[0] ?? url;
    if (noQuery.includes('#')) {
      return (noQuery.split('#').pop() ?? '').split('?')[0] ?? '';
    }
    return noQuery;
  }

  ngAfterViewInit(): void {
    // Keep a local copy so we can map element index -> post.
    this.sub = this.postsService.posts$.subscribe((posts) => {
      this.latestPosts = posts;
      queueMicrotask(() => this.setupIntersectionObserver());
    });
    this.setupIntersectionObserver();
  }

  private setupIntersectionObserver() {
    if (!this.videoEls?.length) return;
    if (this.io) this.io.disconnect();

    this.io = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          const video = entry.target as HTMLVideoElement;
          if (entry.isIntersecting && entry.intersectionRatio >= 0.6) {
            void video.play().catch(() => {
              // Autoplay may be blocked until user interacts; keep controls visible.
            });
          } else {
            video.pause();
          }
        }
      },
      { threshold: [0, 0.6, 1.0] }
    );

    for (const el of this.videoEls) {
      const video = el.nativeElement;
      video.muted = true;
      video.loop = true;
      video.playsInline = true;
      this.io.observe(video);
    }
  }

  trackById(_: number, post: PostVideo) {
    return post.id;
  }

  like(post: PostVideo) {
    if (post.id === 'empty') return;
    this.postsService.toggleLike(post.id);
  }

  async openContact(post: PostVideo) {
    if (post.id === 'empty') return;

    const modal = await this.modalCtrl.create({
      component: ContactSheetComponent,
      breakpoints: [0, 0.5, 0.75, 0.95],
      initialBreakpoint: 0.75,
      componentProps: {
        initialName: post.contact?.name ?? '',
        initialMobile: post.contact?.mobile ?? '',
        initialPlace: post.contact?.place ?? '',
      },
    });

    await modal.present();
    const res = await modal.onDidDismiss<{ name: string; mobile: string; place: string }>();
    if (res.role === 'save' && res.data) {
      this.postsService.setContact(post.id, res.data);
      await this.toast('Contact saved.');
    }
  }

  async share(post: PostVideo) {
    if (post.id === 'empty') return;
    const text = 'Coconut seller video (from cocoTrade)';
    try {
      // Web share when available; else copy to clipboard.
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const navAny: any = navigator;
      if (navAny?.share) {
        await navAny.share({ text });
      } else if (navigator.clipboard) {
        await navigator.clipboard.writeText(text);
        await this.toast('Copied share text.');
      } else {
        await this.toast('Share not supported in this browser.');
      }
    } catch {
      // user canceled share
    }
  }

  ngOnDestroy(): void {
    this.io?.disconnect();
    this.sub?.unsubscribe();
    this.routeSub?.unsubscribe();
  }

  private getEmptyStatePosts(): PostVideo[] {
    // Simple placeholder UI (no real video) until you add first post.
    return [
      {
        id: 'empty',
        createdAt: Date.now(),
        durationSec: 0,
        objectUrl: '',
        likes: 0,
        commentsCount: 0,
      },
    ];
  }

  private async toast(message: string) {
    const t = await this.toastCtrl.create({ message, duration: 1500, position: 'bottom' });
    await t.present();
  }
}

