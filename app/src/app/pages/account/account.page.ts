import { Component, OnInit } from '@angular/core';
import { Observable, of, switchMap } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { PostVideo, PostsService } from '../../services/posts.service';
import { TabShellSyncService } from '../../services/tab-shell-sync.service';
import { AlertController, ToastController } from '@ionic/angular';

@Component({
  selector: 'app-account',
  templateUrl: './account.page.html',
  styleUrls: ['./account.page.scss'],
  standalone: false,
})
export class AccountPage implements OnInit {
  isEditing = false;
  newName = '';
  activeSegment: 'profile' | 'settings' | 'report' = 'profile';

  userPosts$: Observable<PostVideo[]> = of([]);

  // Report fields
  reportType = 'suggestion';
  reportMessage = '';

  constructor(
    public readonly auth: AuthService,
    private readonly postsService: PostsService,
    private readonly tabShellSync: TabShellSyncService,
    private readonly alertCtrl: AlertController,
    private readonly toastCtrl: ToastController
  ) {}

  ngOnInit(): void {
    this.tabShellSync.scheduleSync();
    this.userPosts$ = this.auth.user$.pipe(
      switchMap(user => user ? this.postsService.getUserPosts(user.email) : of([]))
    );
  }

  async signIn() {
    await this.auth.login();
    this.auth.user$.subscribe(user => {
      if (user) {
        this.isEditing = true;
        this.newName = user.displayName;
      }
    }).unsubscribe();
  }

  startEdit() {
    this.auth.user$.subscribe(user => {
      if (user) {
        this.newName = user.displayName;
        this.isEditing = true;
      }
    }).unsubscribe();
  }

  saveName() {
    if (this.newName.trim()) {
      this.auth.updateDisplayName(this.newName.trim());
    }
    this.isEditing = false;
    this.activeSegment = 'profile';
  }

  async markAsSold(postId: string) {
    const alert = await this.alertCtrl.create({
      header: 'Mark as Sold?',
      message: 'This post will be removed from the public feed.',
      buttons: [
        { text: 'Cancel', role: 'cancel' },
        {
          text: 'Confirm',
          handler: () => {
            this.postsService.markAsSold(postId);
          }
        }
      ]
    });
    await alert.present();
  }

  async deletePost(postId: string) {
    const alert = await this.alertCtrl.create({
      header: 'Delete Post?',
      message: 'This action cannot be undone.',
      buttons: [
        { text: 'Cancel', role: 'cancel' },
        {
          text: 'Delete',
          cssClass: 'alert-danger',
          handler: () => {
            this.postsService.deletePost(postId);
          }
        }
      ]
    });
    await alert.present();
  }

  async submitReport() {
    if (!this.reportMessage.trim()) return;

    this.auth.user$.subscribe(async user => {
      if (user) {
        await this.postsService.submitReport({
          type: this.reportType,
          message: this.reportMessage,
          userEmail: user.email
        });

        const t = await this.toastCtrl.create({
          message: 'Report submitted successfully. Thank you!',
          duration: 2000,
          position: 'bottom'
        });
        await t.present();

        this.reportMessage = '';
        this.activeSegment = 'profile';
      }
    }).unsubscribe();
  }
}
