import { Component, ViewChild, ElementRef } from '@angular/core';
import { ToastController } from '@ionic/angular';
import { PostsService } from '../../services/posts.service';

@Component({
  selector: 'app-post-ad',
  templateUrl: './post-ad.page.html',
  styleUrls: ['./post-ad.page.scss'],
  standalone: false,
})
export class PostAdPage {
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;
  @ViewChild('cameraInput') cameraInput!: ElementRef<HTMLInputElement>;
  
  caption = '';
  selectedObjectUrl: string | null = null;
  durationSec: number | null = null;
  busy = false;

  constructor(
    private readonly postsService: PostsService,
    private readonly toastCtrl: ToastController
  ) {}

  selectVideoFile(): void {
    this.fileInput.nativeElement.click();
  }

  selectCameraFile(): void {
    this.cameraInput.nativeElement.click();
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