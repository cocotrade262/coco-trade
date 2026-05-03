import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { IonicModule } from '@ionic/angular';

import { TabsPageRoutingModule } from './tabs-routing.module';

import { TabsPage } from './tabs.page';
import { VideoFeedPageModule } from '../video-feed/video-feed.module';
import { PostAdPageModule } from '../post-ad/post-ad.module';
import { AccountPageModule } from '../account/account.module';

@NgModule({
  imports: [
    CommonModule,
    IonicModule,
    TabsPageRoutingModule,
    VideoFeedPageModule,
    PostAdPageModule,
    AccountPageModule,
  ],
  declarations: [TabsPage],
})
export class TabsPageModule {}
