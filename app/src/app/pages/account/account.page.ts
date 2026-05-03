import { Component, OnInit } from '@angular/core';
import { TabShellSyncService } from '../../services/tab-shell-sync.service';

@Component({
  selector: 'app-account',
  templateUrl: './account.page.html',
  styleUrls: ['./account.page.scss'],
  standalone: false,
})
export class AccountPage implements OnInit {
  constructor(private readonly tabShellSync: TabShellSyncService) {}

  ngOnInit(): void {
    this.tabShellSync.scheduleSync();
  }
}
