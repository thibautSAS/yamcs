import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material';
import { Alarm, EditAlarmOptions } from '@yamcs/client';
import { YamcsService } from '../../core/services/YamcsService';

@Component({
  selector: 'app-acknowledge-alarm-dialog',
  templateUrl: './AcknowledgeAlarmDialog.html',
})
export class AcknowledgeAlarmDialog {

  formGroup: FormGroup;

  constructor(
    private dialogRef: MatDialogRef<AcknowledgeAlarmDialog>,
    formBuilder: FormBuilder,
    private yamcs: YamcsService,
    @Inject(MAT_DIALOG_DATA) readonly data: any,
  ) {
    this.formGroup = formBuilder.group({
      'comment': undefined,
    });
  }

  async acknowledge() {
    const alarms = this.data.alarms as Alarm[];
    const comment = this.formGroup.get('comment')!.value;

    for (const alarm of alarms) {
      const processor = this.yamcs.getProcessor();
      const options: EditAlarmOptions = {
        state: 'acknowledged',
      };
      if (comment) {
        options.comment = comment;
      }
      this.yamcs.getInstanceClient()!.editAlarm(processor.name, alarm.parameter.qualifiedName, alarm.seqNum, options);
    }
    this.dialogRef.close();
  }
}
