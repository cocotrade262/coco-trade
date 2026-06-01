import { Injectable, inject } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { GoogleAuth } from '@codetrix-studio/capacitor-google-auth';
import { isPlatform } from '@ionic/angular';
import {
  Auth,
  signInWithPopup,
  GoogleAuthProvider,
  signOut,
  user as firebaseUser,
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  updateProfile
} from '@angular/fire/auth';
import { registerPlugin, Capacitor } from '@capacitor/core';
import { App } from '@capacitor/app';

export interface NativeAuthPlugin {
  login(): Promise<void>;
  getCurrentUser(): Promise<{ uid: string; email: string; displayName: string; photoUrl: string; idToken: string }>;
  logout(): Promise<void>;
}

const NativeAuth = registerPlugin<NativeAuthPlugin>('NativeAuth');

export interface UserProfile {
  uid: string;
  email: string;
  displayName: string;
  photoUrl?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly fbAuth = inject(Auth);

  // Use a new storage key to invalidate all legacy mock sessions
  private static readonly AUTH_KEY = 'cocotrade_v1_auth';

  private readonly _user$ = new BehaviorSubject<UserProfile | null>(this.getStoredUser());
  readonly user$ = this._user$.asObservable();

  constructor() {
    this.initialize();

    // Listen to Firebase Auth state changes
    firebaseUser(this.fbAuth).subscribe(fbUser => {
      if (fbUser) {
        this.updateUserState({
          id: fbUser.uid,
          email: fbUser.email,
          displayName: fbUser.displayName,
          imageUrl: fbUser.photoURL
        });
      }
    });

    // 1. Absolute Fallback: Listen for App URL Opens (Deep Links)
    if (Capacitor.isNativePlatform()) {
      App.addListener('appUrlOpen', data => {
        console.log('App opened with URL:', data.url);
        if (data.url.includes('auth-callback') || data.url.includes('googleusercontent.apps')) {
          // Force a state refresh from the native layer
          this.refreshSession();
        }
      });
    }
  }

  private async initialize() {
    await this.refreshSession();

    if (!isPlatform('capacitor')) {
      try {
        GoogleAuth.initialize({
          clientId: '274853330536-vlt0nphh115t707f1o90q9l56asolp3q.apps.googleusercontent.com',
          scopes: ['profile', 'email'],
          grantOfflineAccess: true,
        });
      } catch (e) {
        console.warn('GoogleAuth.initialize failed or already initialized', e);
      }
    }

    // Attempt to refresh/verify existing session on load
    try {
      const user = await GoogleAuth.refresh();
      if (user) {
        this.updateUserState(user);
      } else {
        // If refresh fails or returns null, ensure local state is cleared
        this.clearUserState();
      }
    } catch (e) {
      console.log('No active session found on load');
      // If we had a stored user but refresh failed, we might want to clear it
      // to avoid the "mock user" appearance if it was stale.
      this.clearUserState();
    }
  }

  async loginWithEmail(email: string, pass: string) {
    try {
      const result = await signInWithEmailAndPassword(this.fbAuth, email, pass);
      if (result.user) {
        this.updateUserState(result.user);
      }
    } catch (error: any) {
      console.error('Login Error', error);
      throw error;
    }
  }

  async signUpWithEmail(email: string, pass: string, name: string) {
    try {
      const result = await createUserWithEmailAndPassword(this.fbAuth, email, pass);
      if (result.user) {
        await updateProfile(result.user, { displayName: name });
        this.updateUserState(result.user);
      }
    } catch (error: any) {
      console.error('Signup Error', error);
      throw error;
    }
  }

  async login() {
    if (isPlatform('capacitor') && isPlatform('android')) {
      // Native Android flow
      try {
        await NativeAuth.login();
      } catch (error: any) {
        console.error('Native Auth Error', error);
      }
      return;
    }

    // Web Browser or other non-Android Capacitor platforms
    try {
      const provider = new GoogleAuthProvider();
      const result = await signInWithPopup(this.fbAuth, provider);
      if (result.user) {
        this.updateUserState({
          id: result.user.uid,
          email: result.user.email,
          displayName: result.user.displayName,
          imageUrl: result.user.photoURL
        });

        // 2. In-App Web Browser Close Hack: If we are in a popup, try to self-close
        try {
          if (window.opener) {
            window.close();
          }
        } catch (e) {}
      }
    } catch (error: any) {
      // If it's a capacitor environment but not Android, we could try GoogleAuth.signIn()
      // but the user specifically asked for Web Auth fallback to fix the github.io preview.
      console.error('Firebase Web Auth Error', error);
      alert('Login failed: ' + (error.message || 'Unknown error'));
    }
  }

  private async refreshSession() {
    if (isPlatform('capacitor') && isPlatform('android')) {
      try {
        const nativeUser = await NativeAuth.getCurrentUser();
        if (nativeUser) {
           this.updateUserState({
             id: nativeUser.uid,
             email: nativeUser.email,
             displayName: nativeUser.displayName,
             imageUrl: nativeUser.photoUrl
           });
        }
      } catch (e) {}
    }

    if (!isPlatform('capacitor')) {
      try {
        GoogleAuth.initialize({
          clientId: '274853330536-vlt0nphh115t707f1o90q9l56asolp3q.apps.googleusercontent.com',
          scopes: ['profile', 'email'],
          grantOfflineAccess: true,
        });
      } catch (e) {
        console.warn('GoogleAuth.initialize failed or already initialized', e);
      }
    }

    // Attempt to refresh/verify existing session on load
    try {
      const user = await GoogleAuth.refresh();
      if (user) {
        this.updateUserState(user);
      } else if (!isPlatform('android')) {
        // If refresh fails or returns null, ensure local state is cleared (non-android only as android is checked above)
        this.clearUserState();
      }
    } catch (e) {
      console.log('No active session found on load');
      if (!isPlatform('android')) {
        this.clearUserState();
      }
    }
  }

  private updateUserState(googleUser: any) {
    const user: UserProfile = {
      uid: googleUser.id || googleUser.uid,
      email: googleUser.email,
      displayName: googleUser.displayName || googleUser.name || googleUser.email.split('@')[0],
      photoUrl: googleUser.imageUrl
    };
    // 3. Hardcode State Synchronization: Save to localStorage immediately
    localStorage.setItem(AuthService.AUTH_KEY, JSON.stringify(user));
    this._user$.next(user);
  }

  private clearUserState() {
    localStorage.removeItem(AuthService.AUTH_KEY);
    // Also remove the old key if it exists
    localStorage.removeItem('coco_user');
    this._user$.next(null);
  }

  updateDisplayName(name: string) {
    const current = this._user$.value;
    if (current) {
      const updated = { ...current, displayName: name };
      localStorage.setItem(AuthService.AUTH_KEY, JSON.stringify(updated));
      this._user$.next(updated);
    }
  }

  async logout() {
    try {
      if (isPlatform('capacitor') && isPlatform('android')) {
        await NativeAuth.logout();
      }
      await signOut(this.fbAuth);
      await GoogleAuth.signOut();
    } catch (e) {}
    this.clearUserState();

    // Clear legacy redirect hints if any
    localStorage.removeItem('coco_native_bridge');
  }

  async getIdToken(): Promise<string | null> {
    const user = this.fbAuth.currentUser;
    if (user) {
      return user.getIdToken(true);
    }
    return null;
  }

  private getStoredUser(): UserProfile | null {
    const stored = localStorage.getItem(AuthService.AUTH_KEY);
    if (stored) {
       try {
         const user = JSON.parse(stored);
         // Basic validation: ensure it's not the old mock user
         if (user.email === 'user@gmail.com' && user.uid === 'mock_uid_123') {
           return null;
         }
         return user;
       } catch (e) {
         return null;
       }
    }
    return null;
  }
}
