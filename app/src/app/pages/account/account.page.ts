import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { TabShellSyncService } from '../../services/tab-shell-sync.service';

@Component({
  selector: 'app-account',
  templateUrl: './account.page.html',
  styleUrls: ['./account.page.scss'],
  standalone: false,
})
export class AccountPage implements OnInit {
  isEditing = false;
  newName = '';

  constructor(
    public readonly auth: AuthService,
    private readonly tabShellSync: TabShellSyncService
  ) {}

  ngOnInit(): void {
    this.tabShellSync.scheduleSync();
  }

  signIn() {
    // Simulate Google Sign-In with a mock email
    const mockEmail = 'user@gmail.com';
    const mockName = mockEmail.split('@')[0];
    this.auth.login(mockEmail, mockName);
    this.isEditing = true;
    this.newName = mockName;
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
  }
}
