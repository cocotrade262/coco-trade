import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { VideoFeedPage } from './video-feed.page';
import { FormsModule } from '@angular/forms';
import { ContactSheetComponent } from './contact-sheet.component';

@NgModule({
  imports: [CommonModule, FormsModule, IonicModule],
  declarations: [VideoFeedPage, ContactSheetComponent],
  exports: [VideoFeedPage],
})
export class VideoFeedPageModule {}

