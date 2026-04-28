import { Component, Input } from '@angular/core';
import { ModalController, ToastController } from '@ionic/angular';

@Component({
  selector: 'app-contact-sheet',
  templateUrl: './contact-sheet.component.html',
  styleUrls: ['./contact-sheet.component.scss'],
  standalone: false,
})
export class ContactSheetComponent {
  @Input({ required: true }) initialName = '';
  @Input({ required: true }) initialMobile = '';
  @Input({ required: true }) initialPlace = '';

  name = '';
  mobile = '';
  place = '';

  constructor(
    private readonly modalCtrl: ModalController,
    private readonly toastCtrl: ToastController
  ) {}

  ionViewWillEnter() {
    this.name = this.initialName;
    this.mobile = this.initialMobile;
    this.place = this.initialPlace;
  }

  async save() {
    const name = this.name.trim();
    const mobile = this.mobile.trim();
    const place = this.place.trim();

    if (!name || !mobile || !place) {
      await this.toast('Please enter name, mobile and place.');
      return;
    }

    // very light validation for now
    if (!/^[0-9+\-\s]{8,20}$/.test(mobile)) {
      await this.toast('Please enter a valid mobile number.');
      return;
    }

    await this.modalCtrl.dismiss({ name, mobile, place }, 'save');
  }

  cancel() {
    void this.modalCtrl.dismiss(null, 'cancel');
  }

  private async toast(message: string) {
    const t = await this.toastCtrl.create({ message, duration: 1700, position: 'bottom' });
    await t.present();
  }
}

