import { Component, OnInit } from '@angular/core';
import { ViewDidEnter } from '@ionic/angular';
import { TabShellSyncService } from '../../services/tab-shell-sync.service';

@Component({
  selector: 'app-account',
  templateUrl: './account.page.html',
  styleUrls: ['./account.page.scss'],
  standalone: false,
})
export class AccountPage implements OnInit, ViewDidEnter {
  constructor(private readonly tabShellSync: TabShellSyncService) {}

  ngOnInit() {}

  ionViewDidEnter(): void {
    this.tabShellSync.scheduleSync();
  }
}
