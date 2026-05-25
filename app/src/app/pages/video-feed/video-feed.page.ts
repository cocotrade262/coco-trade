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

  isCommentsOpen = false;
  activePostForComments?: PostVideo;
  isMuted = true;
  showUploadStatus = false;
  uploadFinished = false;

  private videoStates = new Map<string, { paused: boolean; progress: number }>();

  constructor(
    public readonly postsService: PostsService,
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
          const postId = this.getPostIdFromVideo(video);
          if (entry.isIntersecting && entry.intersectionRatio >= 0.6) {
            video.muted = this.isMuted;
            void video.play().catch(() => {
              // Autoplay may be blocked until user interacts
            });
            if (postId) this.setVideoState(postId, { paused: false });
          } else {
            video.pause();
            if (postId) this.setVideoState(postId, { paused: true });
          }
        }
      },
      { threshold: [0, 0.6, 1.0] }
    );

    for (const el of this.videoEls) {
      const video = el.nativeElement;
      video.muted = this.isMuted;
      video.loop = true;
      video.playsInline = true;
      this.io.observe(video);
    }
  }

  private getPostIdFromVideo(video: HTMLVideoElement): string | undefined {
    // This is a bit brittle, but works if we assume one video per post
    // Better way would be to pass the post object to the component and use it
    const el = this.videoEls.find((v) => v.nativeElement === video);
    if (!el) return undefined;
    // We can't easily get the post object here without more structure
    // Let's rely on event emitters or data attributes
    return video.getAttribute('data-post-id') || undefined;
  }

  trackById(_: number, post: PostVideo) {
    return post.id;
  }

  toggleMute(event: Event) {
    event.stopPropagation();
    this.isMuted = !this.isMuted;
    for (const el of this.videoEls) {
      el.nativeElement.muted = this.isMuted;
    }
  }

  togglePlay(postId: string, event: Event) {
    event.stopPropagation();
    const video = this.videoEls.find((v) => v.nativeElement.getAttribute('data-post-id') === postId)?.nativeElement;
    if (video) {
      if (video.paused) {
        void video.play();
        this.setVideoState(postId, { paused: false });
      } else {
        video.pause();
        this.setVideoState(postId, { paused: true });
      }
    }
  }

  onTimeUpdate(postId: string, event: Event) {
    const video = event.target as HTMLVideoElement;
    const progress = (video.currentTime / video.duration) * 100;
    this.setVideoState(postId, { progress });
  }

  isPaused(postId: string): boolean {
    return this.videoStates.get(postId)?.paused ?? true;
  }

  getProgress(postId: string): number {
    return this.videoStates.get(postId)?.progress ?? 0;
  }

  private setVideoState(postId: string, state: Partial<{ paused: boolean; progress: number }>) {
    const current = this.videoStates.get(postId) || { paused: true, progress: 0 };
    this.videoStates.set(postId, { ...current, ...state });
  }

  openComments(post: PostVideo) {
    if (post.id === 'empty') return;
    this.activePostForComments = post;
    this.isCommentsOpen = true;
  }

  async openContact(post: PostVideo) {
    if (post.id === 'empty') return;

    const modal = await this.modalCtrl.create({
      component: ContactSheetComponent,
      breakpoints: [0, 0.5, 0.75, 0.95],
      initialBreakpoint: 0.75,
      componentProps: {
        initialName: post.name || '',
        initialMobile: post.mobile || '',
        initialPlace: post.area || '',
        initialCost: post.cost || '',
      },
    });

    await modal.present();
    const res = await modal.onDidDismiss<{ name: string; mobile: string; place: string; cost: string }>();
    if (res.role === 'save' && res.data) {
      this.postsService.updatePostDetails(post.id, {
        name: res.data.name,
        mobile: res.data.mobile,
        area: res.data.place,
        cost: res.data.cost,
      });
      await this.toast('Details updated.');
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
      },
    ];
  }

  private async toast(message: string) {
    const t = await this.toastCtrl.create({ message, duration: 1500, position: 'bottom' });
    await t.present();
  }
}
