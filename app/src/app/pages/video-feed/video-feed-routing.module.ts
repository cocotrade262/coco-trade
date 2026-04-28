import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { VideoFeedPage } from './video-feed.page';

const routes: Routes = [
  {
    path: '',
    component: VideoFeedPage,
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class VideoFeedPageRoutingModule {}

