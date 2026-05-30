import { Injectable } from '@angular/core';
import {
  Database,
  ref as dbRef,
  push,
  set,
  onValue,
  update,
  remove,
  query as dbQuery,
  orderByChild
} from '@angular/fire/database';
import { BehaviorSubject, map, Observable } from 'rxjs';
import { AuthService } from './auth.service';

export type PostVideo = {
  id: string;
  createdAt: number;
  durationSec: number;
  objectUrl: string; // This will be the Cloudinary URL
  caption?: string;
  name?: string;
  mobile?: string;
  area?: string;
  cost?: string;
  authorName?: string;
  uploadedBy?: string; // Authentic Firebase UID
  isSold?: boolean;
};

@Injectable({ providedIn: 'root' })
export class PostsService {
  private readonly _posts$ = new BehaviorSubject<PostVideo[]>([]);
  private readonly _uploadProgress$ = new BehaviorSubject<number | null>(null);

  readonly posts$ = this._posts$.asObservable().pipe(
    map(posts => posts.filter(p => !p.isSold))
  );

  readonly uploadProgress$ = this._uploadProgress$.asObservable();

  constructor(
    private auth: AuthService,
    private db: Database
  ) {
    this.loadPosts();
  }

  private loadPosts() {
    const postsRef = dbRef(this.db, 'UserVideos');
    const q = dbQuery(postsRef, orderByChild('createdAt'));
    onValue(q, (snapshot) => {
      const data = snapshot.val();
      const posts: PostVideo[] = [];
      if (data) {
        Object.keys(data).forEach(key => {
          const item = data[key];
          posts.push({ id: key, ...item });
        });
      }
      this._posts$.next(posts.reverse());
    });
  }

  async addVideoPost(params: {
    file: File | Blob;
    durationSec: number;
    caption?: string;
    name?: string;
    mobile?: string;
    area?: string;
    cost?: string;
    authorName?: string;
  }) {
    let currentUser: any;
    this.auth.user$.subscribe(user => {
      currentUser = user;
    }).unsubscribe();

    if (!currentUser) {
       throw new Error('AUTH_REQUIRED');
    }

    const authorId = currentUser.uid;

    // 1. Upload to Cloudinary via Signed/Unsigned REST API
    const cloudName = 'dt8dfsjjv';
    const uploadPreset = 'ml_default';

    const formData = new FormData();
    formData.append('file', params.file);
    formData.append('upload_preset', uploadPreset);
    formData.append('resource_type', 'video');

    const xhr = new XMLHttpRequest();
    xhr.open('POST', `https://api.cloudinary.com/v1_1/${cloudName}/video/upload`, true);

    return new Promise<void>((resolve, reject) => {
      xhr.upload.onprogress = (e) => {
        if (e.lengthComputable) {
          const progress = (e.loaded / e.total) * 100;
          this._uploadProgress$.next(progress);
        }
      };

      xhr.onload = async () => {
        if (xhr.status === 200) {
          const response = JSON.parse(xhr.responseText);
          const downloadUrl = response.secure_url;

          // 2. Save to Firebase Realtime Database
          const postData = {
            createdAt: Date.now(),
            durationSec: params.durationSec,
            objectUrl: downloadUrl,
            caption: params.caption,
            name: params.name,
            mobile: params.mobile,
            area: params.area,
            cost: params.cost,
            authorName: params.authorName,
            uploadedBy: authorId,
            isSold: false,
            timestamp: Date.now()
          };

          const newUserVideoRef = push(dbRef(this.db, 'UserVideos'));
          await set(newUserVideoRef, postData);

          this._uploadProgress$.next(null);
          resolve();
        } else {
          this._uploadProgress$.next(null);
          let errorMsg = 'Cloudinary upload failed';
          try {
            const resp = JSON.parse(xhr.responseText);
            if (resp.error && resp.error.message) {
              errorMsg = resp.error.message;
            }
          } catch (e) {}
          reject(new Error(errorMsg));
        }
      };

      xhr.onerror = () => {
        this._uploadProgress$.next(null);
        reject(new Error('Network error during upload'));
      };

      xhr.send(formData);
    });
  }

  async updatePostDetails(postId: string, details: Partial<PostVideo>) {
    const postRef = dbRef(this.db, `UserVideos/${postId}`);
    return update(postRef, details);
  }

  async markAsSold(postId: string) {
    return this.updatePostDetails(postId, { isSold: true });
  }

  async deletePost(postId: string) {
    const postRef = dbRef(this.db, `UserVideos/${postId}`);
    return remove(postRef);
  }

  getUserPosts(userId: string): Observable<PostVideo[]> {
    return this._posts$.asObservable().pipe(
      map(posts => posts.filter(p => p.uploadedBy === userId))
    );
  }

  async submitReport(report: { type: string, message: string, userEmail: string }) {
    const reportsRef = dbRef(this.db, 'reports');
    const newReportRef = push(reportsRef);
    await set(newReportRef, {
      ...report,
      createdAt: Date.now(),
      to: 'cocotrade262@gmail.com'
    });

    const body = encodeURIComponent(`User: ${report.userEmail}\n\n${report.message}`);
    const mailto = `mailto:cocotrade262@gmail.com?subject=Report/Suggestion: ${report.type}&body=${body}`;
    window.open(mailto, '_blank');
  }
}
