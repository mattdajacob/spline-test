/*
 * Copyright 2017 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Component } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from 'src/app/model/app-state';
import { Observable } from 'rxjs';
import { OperationDetailsVM } from 'src/app/model/viewModels/operationDetailsVM';

@Component({
  selector: 'lineage-details',
  templateUrl: './lineage-details.component.html',
  styleUrls: ['./lineage-details.component.less']
})
export class LineageDetailsComponent {

  constructor(
    private store: Store<AppState>
  ) { }

  public getDetailsInfo = (): Observable<OperationDetailsVM> => {
    return this.store.select('detailsInfos')
  }
}
