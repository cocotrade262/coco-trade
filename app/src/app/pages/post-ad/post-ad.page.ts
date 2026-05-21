import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ToastController } from '@ionic/angular';
import { AuthService } from '../../services/auth.service';
import { PostsService } from '../../services/posts.service';
import { TabShellSyncService } from '../../services/tab-shell-sync.service';

@Component({
  selector: 'app-post-ad',
  templateUrl: './post-ad.page.html',
  styleUrls: ['./post-ad.page.scss'],
  standalone: false,
})
export class PostAdPage implements OnInit {
  caption = '';
  name = '';
  area = '';
  mobile = '';
  cost = '';
  selectedObjectUrl: string | null = null;
  durationSec: number | null = null;
  busy = false;

  constructor(
    private readonly postsService: PostsService,
    private readonly auth: AuthService,
    private readonly toastCtrl: ToastController,
    private readonly router: Router,
    private readonly tabShellSync: TabShellSyncService
  ) {}

  ngOnInit(): void {
    this.tabShellSync.scheduleSync();
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

      this.selectedObjectUrl = objectUrl;
      this.durationSec = duration;
    } finally {
      this.busy = false;
    }
  }

  async publish() {
    if (!this.selectedObjectUrl || !this.durationSec) return;

    let finalUrl = this.selectedObjectUrl;
    let finalDuration = this.durationSec;

    if (this.durationSec > 30) {
      finalUrl += '#t=0,30';
      finalDuration = 30;
    }

  let authorName: string | undefined;
  this.auth.user$.subscribe(user => {
    if (user) {
      authorName = user.displayName;
    }
  }).unsubscribe();

    this.postsService.addVideoPost({
      objectUrl: finalUrl,
      durationSec: finalDuration,
      caption: this.caption,
      name: this.name,
      area: this.area,
      mobile: this.mobile,
      cost: this.cost,
    authorName: authorName
    });

    this.selectedObjectUrl = null;
    this.durationSec = null;
    this.caption = '';
    this.name = '';
    this.area = '';
    this.mobile = '';
    this.cost = '';

    // Set a flag in session storage to trigger upload status on feed
    sessionStorage.setItem('post_uploading', 'true');

    await this.router.navigate(['/tabs/feed']);

    // Reset fields
    this.caption = '';
    this.name = '';
    this.area = '';
    this.mobile = '';
    this.cost = '';
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
