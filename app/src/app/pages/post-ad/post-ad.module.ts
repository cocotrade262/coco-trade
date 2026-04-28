import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { PostAdPageRoutingModule } from './post-ad-routing.module';

import { PostAdPage } from './post-ad.page';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    PostAdPageRoutingModule
  ],
  declarations: [PostAdPage]
})
export class PostAdPageModule {}
