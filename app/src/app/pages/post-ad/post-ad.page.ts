import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { ToastController } from '@ionic/angular';
import { AuthService } from '../../services/auth.service';
import { PostsService } from '../../services/posts.service';
import { TabShellSyncService } from '../../services/tab-shell-sync.service';
import { checkAndCompressVideo } from '../../utils/compression.util';

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
  selectedFile: File | null = null;
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
      this.selectedFile = file;
      this.durationSec = duration;
    } finally {
      this.busy = false;
    }
  }

  async publish() {
    if (!this.selectedFile || !this.durationSec) return;

    const fileToUpload = this.selectedFile;
    const durationToUpload = this.durationSec;
    const captionToUpload = this.caption;
    const nameToUpload = this.name;
    const mobileToUpload = this.mobile;
    const areaToUpload = this.area;
    const costToUpload = this.cost;

    let authorName: string | undefined;
    this.auth.user$.subscribe(user => {
      if (user) authorName = user.email.split('@')[0];
    }).unsubscribe();

    // 1. Reset local state immediately
    this.selectedObjectUrl = null;
    this.selectedFile = null;
    this.durationSec = null;
    this.caption = '';
    this.name = '';
    this.area = '';
    this.mobile = '';
    this.cost = '';

    // 2. Navigate to feed immediately
    await this.router.navigate(['/tabs/feed']);

    // 3. Start upload in background
    try {
      let finalFile: File | Blob = await checkAndCompressVideo(fileToUpload);
      let finalDuration = durationToUpload;
      if (finalDuration > 30) finalDuration = 30;

      await this.postsService.addVideoPost({
        file: finalFile,
        durationSec: finalDuration,
        caption: captionToUpload,
        name: nameToUpload,
        area: areaToUpload,
        mobile: mobileToUpload,
        cost: costToUpload,
        authorName: authorName
      });
    } catch (e: any) {
      console.error('Publish failed', e);
      if (e.message === 'AUTH_REQUIRED') {
        await this.toast('Please sign in with Google first.');
        await this.router.navigate(['/tabs/account']);
      } else {
        await this.toast('Failed to publish video. Try again.');
      }
    } finally {
      this.busy = false;
    }

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
