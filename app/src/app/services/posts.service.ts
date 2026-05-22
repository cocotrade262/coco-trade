import { Injectable } from '@angular/core';
import {
  Firestore,
  collection,
  addDoc,
  collectionData,
  doc,
  updateDoc,
  deleteDoc,
  query,
  where,
  orderBy
} from '@angular/fire/firestore';
import {
  Storage,
  ref,
  uploadBytes,
  getDownloadURL
} from '@angular/fire/storage';
import { BehaviorSubject, map, Observable, from, switchMap, of } from 'rxjs';
import { AuthService } from './auth.service';

export type PostVideo = {
  id?: string;
  createdAt: number;
  durationSec: number;
  objectUrl: string; // This will now be the Firebase Storage URL
  caption?: string;
  name?: string;
  mobile?: string;
  area?: string;
  cost?: string;
  authorName?: string;
  authorId?: string;
  isSold?: boolean;
};

@Injectable({ providedIn: 'root' })
export class PostsService {
  private readonly _posts$ = new BehaviorSubject<PostVideo[]>([]);

  // Public feed: Not sold, ordered by date
  readonly posts$ = this._posts$.asObservable().pipe(
    map(posts => posts.filter(p => !p.isSold))
  );

  constructor(
    private auth: AuthService,
    private firestore: Firestore,
    private storage: Storage
  ) {
    this.loadPosts();
  }

  private loadPosts() {
    const postsCol = collection(this.firestore, 'posts');
    const q = query(postsCol, orderBy('createdAt', 'desc'));
    collectionData(q, { idField: 'id' }).subscribe(posts => {
      this._posts$.next(posts as PostVideo[]);
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
    let authorId: string | undefined;
    this.auth.user$.subscribe(user => {
      if (user) authorId = user.email;
    }).unsubscribe();

    // 1. Upload to Storage
    const filePath = `videos/${Date.now()}_${authorId || 'anon'}`;
    const storageRef = ref(this.storage, filePath);
    const uploadTask = await uploadBytes(storageRef, params.file);
    const downloadUrl = await getDownloadURL(uploadTask.ref);

    // 2. Save to Firestore
    const post: PostVideo = {
      createdAt: Date.now(),
      durationSec: params.durationSec,
      objectUrl: downloadUrl,
      caption: params.caption,
      name: params.name,
      mobile: params.mobile,
      area: params.area,
      cost: params.cost,
      authorName: params.authorName,
      authorId: authorId,
      isSold: false
    };

    const postsCol = collection(this.firestore, 'posts');
    return addDoc(postsCol, post);
  }

  async markAsSold(postId: string) {
    const postDoc = doc(this.firestore, `posts/${postId}`);
    return updateDoc(postDoc, { isSold: true });
  }

  async deletePost(postId: string) {
    const postDoc = doc(this.firestore, `posts/${postId}`);
    return deleteDoc(postDoc);
  }

  getUserPosts(userId: string): Observable<PostVideo[]> {
    return this._posts$.asObservable().pipe(
      map(posts => posts.filter(p => p.authorId === userId))
    );
  }

  async submitReport(report: { type: string, message: string, userEmail: string }) {
    const reportsCol = collection(this.firestore, 'reports');
    await addDoc(reportsCol, {
      ...report,
      createdAt: Date.now(),
      to: 'cocotrade262@gmail.com'
    });

    // Also trigger mailto as a fallback/direct action
    const body = encodeURIComponent(`User: ${report.userEmail}\n\n${report.message}`);
    const mailto = `mailto:cocotrade262@gmail.com?subject=Report/Suggestion: ${report.type}&body=${body}`;
    window.open(mailto, '_blank');
  }
}
