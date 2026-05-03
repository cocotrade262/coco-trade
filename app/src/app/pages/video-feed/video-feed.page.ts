import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChildren } from '@angular/core';
import { ModalController, ToastController } from '@ionic/angular';
import { map, Observable, Subscription } from 'rxjs';
import { PostsService, PostVideo } from '../../services/posts.service';
import { TabShellSyncService } from '../../services/tab-shell-sync.service';
import { ContactSheetComponent } from './contact-sheet.component';

@Component({
  selector: 'app-video-feed',
  templateUrl: './video-feed.page.html',
  styleUrls: ['./video-feed.page.scss'],
  standalone: false,
})
export class VideoFeedPage implements OnInit, AfterViewInit, OnDestroy {
  readonly posts$: Observable<PostVideo[]> = this.postsService.posts$.pipe(
    map((posts: PostVideo[]) => (posts.length ? posts : this.getEmptyStatePosts()))
  );

  @ViewChildren('videoEl') videoEls!: ElementRef<HTMLVideoElement>[];

  private io?: IntersectionObserver;
  private sub?: Subscription;

  constructor(
    private readonly postsService: PostsService,
    private readonly modalCtrl: ModalController,
    private readonly toastCtrl: ToastController,
    private readonly tabShellSync: TabShellSyncService
  ) {}

  ngOnInit(): void {
    this.tabShellSync.scheduleSync();
  }

  ngAfterViewInit(): void {
    this.sub = this.postsService.posts$.subscribe(() => {
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
  }

  private getEmptyStatePosts(): PostVideo[] {
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
