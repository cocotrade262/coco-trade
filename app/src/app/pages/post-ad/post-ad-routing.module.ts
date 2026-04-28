import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { PostAdPage } from './post-ad.page';

const routes: Routes = [
  {
    path: '',
    component: PostAdPage
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class PostAdPageRoutingModule {}
