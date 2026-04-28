import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { IonicModule } from '@ionic/angular';

import { AdDetailPageRoutingModule } from './ad-detail-routing.module';

import { AdDetailPage } from './ad-detail.page';

@NgModule({
  imports: [
    CommonModule,
    FormsModule,
    IonicModule,
    AdDetailPageRoutingModule
  ],
  declarations: [AdDetailPage]
})
export class AdDetailPageModule {}
