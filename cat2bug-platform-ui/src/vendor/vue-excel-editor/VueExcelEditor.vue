<template>
  <div ref="editor" class="vue-excel-editor" :style="{display: 'inline-block', width}">
    <div class="component-content">
      <!-- No record -->
      <div v-if="localizedLabel.noRecordIndicator && pagingTable.length == 0" class="norecord" :style="{bottom: noFooter? '12px' : '37px'}">
        {{ localizedLabel.noRecordIndicator }}
      </div>

      <div ref="tableContent"
           class="table-content"
           :class="{'no-footer': noFooter}"
           @scroll="tableScroll"
           @mouseover="mouseOver"
           @mouseout="mouseOut">

        <!-- Main Table -->
        <table ref="systable"
              id="systable"
              style="table-layout: fixed; width: 0"
              class="systable"
              :class="{'no-number': noNumCol}"
              ondragenter="event.preventDefault(); event.dataTransfer.dropEffect = 'none'"
              ondragover="event.preventDefault(); event.dataTransfer.dropEffect = 'none'">
          <colgroup>
            <col v-if="!noNumCol" style="width:40px">
            <col v-for="(item, p) in fields" v-show="!item.invisible" :key="p" :style="{width: item.width}">
            <col v-if="vScroller.buttonHeight < vScroller.height" style="width:12px">
          </colgroup>
          <thead class="center-text">
            <tr>
              <th class="center-text first-col"
                  :class="{hide: noNumCol}"
                  style="top: 0">
                <span class="first-col-corner-placeholder" style="width:100%;display:block" aria-hidden="true"></span>
              </th>
              <th v-for="(item, p) in fields"
                  v-show="!item.invisible"
                  :key="`th-${p}`"
                  :colspan="p === fields.length - 1 && vScroller.buttonHeight < vScroller.height ? 2: 1"
                  :class="{'sort-asc-sign': sortPos==p && sortDir==1,
                          'sort-des-sign': sortPos==p && sortDir==-1,
                          'sticky-column': item.sticky}"
                  :style="{left: item.left}"
                  @mousedown="headerClick($event, p)"
                  @contextmenu.prevent="panelFilterClick(item)">
                <div :class="{'filter-sign': columnFilter[p]}">
                  <span :class="{'table-col-header': !noHeaderEdit}" v-html="headerLabel(item.label, item)"></span>
                </div>
                <div class="col-sep"
                    @mousedown="colSepMouseDown"
                    @mouseover="colSepMouseOver"
                    @mouseout="colSepMouseOut">
                  <div class="add-col-btn"> + </div>
                </div>
              </th>
            </tr>
            <tr :class="{hide: !filterRow}">
              <td class="center-text first-col tl-filter"
                  :class="{hide: noNumCol}"
                  style="vertical-align: middle; padding: 0"
                  :style="{top: calCellTop2 + 'px'}"
                  @click="columnFilter = {}">
                <span v-if="Object.keys(columnFilter).length > 0">
                  <svg aria-hidden="true" focusable="false" data-prefix="fas" data-icon="eraser" role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" class="svg-inline--fa fa-eraser fa-w-16 fa-sm"><path fill="currentColor" d="M497.941 273.941c18.745-18.745 18.745-49.137 0-67.882l-160-160c-18.745-18.745-49.136-18.746-67.883 0l-256 256c-18.745 18.745-18.745 49.137 0 67.882l96 96A48.004 48.004 0 0 0 144 480h356c6.627 0 12-5.373 12-12v-40c0-6.627-5.373-12-12-12H355.883l142.058-142.059zm-302.627-62.627l137.373 137.373L265.373 416H150.628l-80-80 124.686-124.686z"></path></svg>
                </span>
                <!--
                <svg v-if="selectedCount==table.length" aria-hidden="true" focusable="false" data-prefix="fas" data-icon="times-circle" role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" class="svg-inline--fa fa-times-circle fa-w-16 fa-sm"><path fill="currentColor" d="M256 8C119 8 8 119 8 256s111 248 248 248 248-111 248-248S393 8 256 8zm121.6 313.1c4.7 4.7 4.7 12.3 0 17L338 377.6c-4.7 4.7-12.3 4.7-17 0L256 312l-65.1 65.6c-4.7 4.7-12.3 4.7-17 0L134.4 338c-4.7-4.7-4.7-12.3 0-17l65.6-65-65.6-65.1c-4.7-4.7-4.7-12.3 0-17l39.6-39.6c4.7-4.7 12.3-4.7 17 0l65 65.7 65.1-65.6c4.7-4.7 12.3-4.7 17 0l39.6 39.6c4.7 4.7 4.7 12.3 0 17L312 256l65.6 65.1z"></path></svg>
                <svg v-else aria-hidden="true" focusable="false" data-prefix="fas" data-icon="check-circle" role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" class="svg-inline--fa fa-check-circle fa-w-16 fa-sm"><path fill="currentColor" d="M504 256c0 136.967-111.033 248-248 248S8 392.967 8 256 119.033 8 256 8s248 111.033 248 248zM227.314 387.314l184-184c6.248-6.248 6.248-16.379 0-22.627l-22.627-22.627c-6.248-6.249-16.379-6.249-22.628 0L216 308.118l-70.059-70.059c-6.248-6.248-16.379-6.248-22.628 0l-22.627 22.627c-6.248 6.248-6.248 16.379 0 22.627l104 104c6.249 6.249 16.379 6.249 22.628.001z"></path></svg>
                -->
              </td>
              <vue-excel-filter v-for="(item, p) in fields"
                                v-show="!item.invisible"
                                :ref="`filter-${item.name}`"
                                :colspan="p === fields.length - 1? 2: 1"
                                :key="`th2-${p}`"
                                v-model="columnFilter[p]"
                                :class="{'sticky-column': item.sticky}"
                                :style="{left: item.left}"
                                class="column-filter" />
            </tr>
          </thead>
          <tbody @mousedown="mouseDown" @dblclick="cellDoubleClick">
            <tr v-if="localizedLabel.noRecordIndicator && pagingTable.length == 0">
              <td colspan="100%" style="height:40px; vertical-align: middle; text-align: center"></td>
            </tr>
            <tr v-else
                v-for="(record, rowPos) in pagingTable"
                :key="rowPos"
                :class="{select: typeof selected[pageTop + rowPos] !== 'undefined'}"
                :style="rowStyle(record)">
              <td class="center-text first-col"
                  :id="`rid-${record.$id}`"
                  :class="{
                    hide: noNumCol,
                    error: rowerr[`rid-${record.$id}`]
                  }"
                  :pos="rowPos"
                  @mouseover="numcolMouseOver"
                  @mousedown="rowLabelMouseDown">
                <span v-html="recordLabel(pageTop + rowPos + 1, record)"></span>
              </td>
              <td v-for="(item, p) in fields"
                  v-show="!item.invisible"
                  :id="`id-${record.$id}-${item.name}`"
                  :cell-RC="`${rowPos}-${item.name}`"
                  :class="{
                    readonly: item.readonly,
                    error: errmsg[`id-${record.$id}-${item.name}`],
                    link: item.link,
                    select: item.options,
                    datepick: item.type == 'date',
                    'sticky-column': item.sticky,
                    'cell-html': !!item.cellHtml
                  }"
                  :key="p"
                  :style="Object.assign(cellStyle(record, item), renderColumnCellStyle(item))"
                  @mouseover="cellMouseOver"
                  @mousemove="cellMouseMove">
                <span
                  v-if="item.cellHtml"
                  class="vue-excel-cell-html"
                  v-html="item.cellHtml(record, item)"
                ></span>
                <template v-else>{{ item.toText(record[item.name]) }}</template>
              </td>
              <td v-if="vScroller.buttonHeight < vScroller.height" class="last-col"></td>
            </tr>
          </tbody>
          <tfoot>
            <tr v-show="pagingTable.length && summaryRow">
              <td class="row-summary first-col">&nbsp;</td>
              <td v-for="(field, p) in fields"
                  v-show="!field.invisible"
                  class="row-summary"
                  :colspan="p === fields.length - 1 && vScroller.buttonHeight < vScroller.height ? 2: 1"
                  :class="{
                    'sticky-column': field.sticky,
                    'summary-column1': p+1 < fields.length && fields[p+1].summary,
                    'summary-column2': field.summary
                  }"
                  :key="`f${p}`"
                  :style="renderColumnCellStyle(field)"
                  >{{ summary[field.name] }}</td>
            </tr>
          </tfoot>
          <slot></slot>
        </table>

        <!-- Tool Tip -->
        <div v-show="tip" ref="tooltip" class="tool-tip">{{ tip }}</div>

        <!-- Text Tip -->
        <div v-show="textTip" ref="texttip" class="text-tip">{{ textTip }}</div>

        <!-- 填充柄拖动预览：围住源格到目标格（同行或同列） -->
        <div v-show="fillDragOverlay.show"
             ref="fillDragOverlay"
             class="fill-drag-overlay"
             :style="fillDragOverlay.style"></div>

        <!-- 矩形区域选：整块外框+底色（盖在表格之上，避免 cell-html 等遮住 td 背景） -->
        <div v-show="cellRangeOverlay.show"
             class="cell-range-overlay"
             :style="cellRangeOverlay.style"></div>
        <div v-for="(seg, si) in rowSelectOverlaySegments"
             :key="'row-sel-ov-' + si"
             v-show="seg.show"
             class="cell-range-overlay"
             :style="seg.style"></div>

        <!-- Editor Square -->
        <div v-show="focused"
             ref="inputSquare"
             class="input-square"
             :class="{
               'input-square--editing': !!inputBoxShow,
               'input-square--range-suppress': suppressInputSquareRangeChrome,
               'input-square--defect-name': currentField && !currentField.readonly && currentField.type !== 'date' && !currentField.options
             }"
             @mousedown="inputSquareClick">
          <div
            :data-vee-field="currentField ? currentField.name : ''"
            style="position: relative; height: 100%; padding: 2px 2px 1px">
            <div
              v-show="!cellRangeDrag && !fillDragActive"
              class="rb-square"
              @mousedown.stop.prevent="fillHandleMouseDown"
            />
            <textarea ref="inputBox"
                      id="inputBox"
                      class="input-box"
                      v-bind="defectNameTextareaRowBind"
                      :style="{opacity: inputBoxShow}"
                      @blur="inputBoxBlur"
                      @input="onInputBoxNativeInput"
                      @mousemove="inputBoxMouseMove"
                      @mousedown="inputBoxMouseDown"
                      trim
                      autocomplete="off"
                      autocorrect="off"
                      autocompitaize="off"
                      :spellcheck="spellcheck"></textarea>
          </div>
        </div>

        <!-- Date Picker -->
        <div ref="dpContainer" v-show="showDatePicker" style="z-index:20; position:fixed">
          <date-picker ref="datepicker" inline v-model="inputDateTime" @input="datepickerClick" valueType="format"></date-picker>
        </div>

        <!-- Waiting scene -->
        <div v-show="processing" ref="frontdrop" class="front-drop">
          <svg aria-hidden="true" focusable="false" data-prefix="fas" data-icon="spinner" role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" class="svg-inline--fa fa-spinner fa-w-16 fa-spin fa-3x"><path fill="currentColor" d="M304 48c0 26.51-21.49 48-48 48s-48-21.49-48-48 21.49-48 48-48 48 21.49 48 48zm-48 368c-26.51 0-48 21.49-48 48s21.49 48 48 48 48-21.49 48-48-21.49-48-48-48zm208-208c-26.51 0-48 21.49-48 48s21.49 48 48 48 48-21.49 48-48-21.49-48-48-48zM96 256c0-26.51-21.49-48-48-48S0 229.49 0 256s21.49 48 48 48 48-21.49 48-48zm12.922 99.078c-26.51 0-48 21.49-48 48s21.49 48 48 48 48-21.49 48-48c0-26.509-21.491-48-48-48zm294.156 0c-26.51 0-48 21.49-48 48s21.49 48 48 48 48-21.49 48-48c0-26.509-21.49-48-48-48zM108.922 60.922c-26.51 0-48 21.49-48 48s21.49 48 48 48 48-21.49 48-48-21.491-48-48-48z"></path></svg>
        </div>
      </div>

      <!-- Vertical Scroll Bar -->
      <div v-show="vScroller.buttonHeight < vScroller.height"
           ref="vScroll"
           class="v-scroll"
           :style="{top: `${vScroller.top}px`, height: `${vScroller.height}px`}"
           @mousedown="vsMouseDown">
        <div ref="vScrollButton"
             class="v-scroll-button"
             :style="{marginTop: `${vScroller.buttonTop}px`, height: `${vScroller.buttonHeight}px`}"
             @mousedown="vsbMouseDown">
          <div v-show="vScroller.runner" class="runner" v-html="vScroller.runner" />
        </div>
      </div>

      <!-- Autocomplete List -->
      <ul ref="autocomplete" v-show="focused && autocompleteInputs.length" class="autocomplete-results">
        <li v-for="(item,i) in autocompleteInputs"
            :key="i"
            :class="{select: autocompleteSelect === i}"
            @mousedown.left.prevent="inputAutocompleteText($event.target.textContent, $event)"
            class="autocomplete-result">{{ item }}</li>
      </ul>

      <!-- Footer -->
      <div ref="footer" class="footer center-text" :class="{hide: noFooter}" style="position:relative" @mousedown="ftMouseDown">
        <div ref="hScroll" class="h-scroll" @mousedown="sbMouseDown" />
        <span class="left-block"></span>
        <span v-show="!noPaging" style="position: absolute; left: 46px">
          <span v-html="localizedLabel.footerLeft(pageTop + 1, pageBottom, table.length)"></span>
        </span>
        <span v-show="!noPaging && pageBottom - pageTop < table.length">
          <template v-if="processing">
            <svg aria-hidden="true" focusable="false" data-prefix="fas" data-icon="spinner" role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" class="svg-inline--fa fa-spinner fa-w-16 fa-spin fa-sm"><path fill="currentColor" d="M304 48c0 26.51-21.49 48-48 48s-48-21.49-48-48 21.49-48 48-48 48 21.49 48 48zm-48 368c-26.51 0-48 21.49-48 48s21.49 48 48 48 48-21.49 48-48-21.49-48-48-48zm208-208c-26.51 0-48 21.49-48 48s21.49 48 48 48 48-21.49 48-48-21.49-48-48-48zM96 256c0-26.51-21.49-48-48-48S0 229.49 0 256s21.49 48 48 48 48-21.49 48-48zm12.922 99.078c-26.51 0-48 21.49-48 48s21.49 48 48 48 48-21.49 48-48c0-26.509-21.491-48-48-48zm294.156 0c-26.51 0-48 21.49-48 48s21.49 48 48 48 48-21.49 48-48c0-26.509-21.49-48-48-48zM108.922 60.922c-26.51 0-48 21.49-48 48s21.49 48 48 48 48-21.49 48-48-21.491-48-48-48z"></path></svg>
            &nbsp;
            <span v-html="localizedLabel.processing" />
          </template>
          <template v-else>
            <a :class="{disabled: pageTop <= 0}" @mousedown="firstPage">
              <svg aria-hidden="true" focusable="false" data-prefix="fas" data-icon="step-backward" role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512" class="svg-inline--fa fa-step-backward fa-w-14 fa-sm"><path fill="currentColor" d="M64 468V44c0-6.6 5.4-12 12-12h48c6.6 0 12 5.4 12 12v176.4l195.5-181C352.1 22.3 384 36.6 384 64v384c0 27.4-31.9 41.7-52.5 24.6L136 292.7V468c0 6.6-5.4 12-12 12H76c-6.6 0-12-5.4-12-12z"></path></svg>
              &nbsp;
              <span v-html="localizedLabel.first" />
            </a>
            &nbsp;|&nbsp;
            <a :class="{disabled: pageTop <= 0}" @mousedown="prevPage">
              <svg aria-hidden="true" focusable="false" data-prefix="fas" data-icon="backward" role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" class="svg-inline--fa fa-backward fa-w-16 fa-sm"><path fill="currentColor" d="M11.5 280.6l192 160c20.6 17.2 52.5 2.8 52.5-24.6V96c0-27.4-31.9-41.8-52.5-24.6l-192 160c-15.3 12.8-15.3 36.4 0 49.2zm256 0l192 160c20.6 17.2 52.5 2.8 52.5-24.6V96c0-27.4-31.9-41.8-52.5-24.6l-192 160c-15.3 12.8-15.3 36.4 0 49.2z"></path></svg>
              &nbsp;
              <span v-html="localizedLabel.previous" />
            </a>
            &nbsp;|&nbsp;
            <a :class="{disabled: pageTop + pageSize >= table.length}" @mousedown="nextPage">
              <span v-html="localizedLabel.next" />
              &nbsp;
              <svg aria-hidden="true" focusable="false" data-prefix="fas" data-icon="forward" role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" class="svg-inline--fa fa-forward fa-w-16 fa-sm"><path fill="currentColor" d="M500.5 231.4l-192-160C287.9 54.3 256 68.6 256 96v320c0 27.4 31.9 41.8 52.5 24.6l192-160c15.3-12.8 15.3-36.4 0-49.2zm-256 0l-192-160C31.9 54.3 0 68.6 0 96v320c0 27.4 31.9 41.8 52.5 24.6l192-160c15.3-12.8 15.3-36.4 0-49.2z"></path></svg>
            </a>
            &nbsp;|&nbsp;
            <a :class="{disabled: pageTop + pageSize >= table.length}" @mousedown="lastPage">
              <span v-html="localizedLabel.last" />
              &nbsp;
              <svg aria-hidden="true" focusable="false" data-prefix="fas" data-icon="step-forward" role="img" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512" class="svg-inline--fa fa-step-forward fa-w-14 fa-sm"><path fill="currentColor" d="M384 44v424c0 6.6-5.4 12-12 12h-48c-6.6 0-12-5.4-12-12V291.6l-195.5 181C95.9 489.7 64 475.4 64 448V64c0-27.4 31.9-41.7 52.5-24.6L312 219.3V44c0-6.6 5.4-12 12-12h48c6.6 0 12 5.4 12 12z"></path></svg>
            </a>
          </template>
        </span>
        <span style="position: absolute; right: 6px">
          <a :class="{disabled: !showSelectedOnly && selectedCount <= 1}" @mousedown="toggleSelectView">
            <span v-html="localizedLabel.footerRight.selected" />
            <span :style="{color: selectedCount>0 ? 'red': 'inherit'}">{{ selectedCount }}</span>
          </a>
          &nbsp;|&nbsp;
          <a :class="{disabled: columnFilterString === '{}'}" @mousedown="toggleFilterView">
            <span v-html="localizedLabel.footerRight.filtered" />
            <span :style="{color: table.length !== filteredValue.length ? 'red': 'inherit'}">{{ table.length }}</span>
          </a>
          &nbsp;|&nbsp;
          <a :class="{disabled: true}">
            <span v-html="localizedLabel.footerRight.loaded" />
            <span>{{ filteredValue.length }}</span>
          </a>
        </span>
      </div>

      <input type="file"
             ref="importFile"
             accept=".xlsx, .xls, xlsm, .csv"
             style="position: absolute; top: 0; left: 0; width:0; height: 0; opacity:0; z-index:-1"
             @keyup="componentTabInto"
             @change="doImport" />

      <panel-filter ref="panelFilter" :n-filter-count="nFilterCount" :localized-label="localizedLabel" />
      <panel-setting ref="panelSetting" v-model="fields" :localized-label="localizedLabel" />
      <panel-find ref="panelFind" :localized-label="localizedLabel" />
    </div>
  </div>
</template>

<script>
import VueExcelFilter from './VueExcelFilter.vue'
import PanelFilter from './PanelFilter.vue'
import PanelSetting from './PanelSetting.vue'
import PanelFind from './PanelFind.vue'
import DatePicker from 'vue2-datepicker'
import XLSX from 'xlsx'
import { escapeHtml } from '@/utils/sanitize-html'

import 'vue2-datepicker/index.css'

export default {
  components: {
    'vue-excel-filter': VueExcelFilter,
    'panel-filter': PanelFilter,
    'panel-setting': PanelSetting,
    'panel-find': PanelFind,
    'date-picker': DatePicker
  },
  props: {
    disablePanelSetting: {
      type: Boolean,
      default() {
        return false;
      }
    },
    disablePanelFilter: {
      type: Boolean,
      default() {
        return false;
      }
    },
    value: {type: Array, default () {return []}},
    rowStyle: {type: Function, default () {return {}}},
    cellStyle: {type: Function, default () {return {}}},
    headerLabel: {
      type: Function,
      default (label) {
        return label
      }
    },
    recordLabel: {                                  // return the row header
      type: Function,
      default (pos) {
        return pos
      }
    },
    noFinding: {type: Boolean, default: false},
    noFindingNext: {type: Boolean, default: false},
    filterRow: {type: Boolean, default: false},
    freeSelect: {type: Boolean, default: false},
    noFooter: {type: Boolean, default: false},
    noPaging: {type: Boolean, default: false},
    noNumCol: {type: Boolean, default: false},
    noMouseScroll: {type: Boolean, default: false},
    page: {type: Number, default: 0},               // prefer page size, auto-cal if not provided
    enterToSouth: {type: Boolean, default: false},  // default enter to south
    nFilterCount: {type: Number, default: 1000},    // show top n values in filter dialog
    height: {type: String, default: ''},
    width: {type: String, default: '100%'},
    wheelSensitivity: {type: Number, default: 30},
    autocomplete: {type: Boolean, default: false},  // Default autocomplete of all columns
    autocompleteCount: {type: Number, default: 50},
    readonly: {type: Boolean, default: false},
    readonlyStyle: {type: Object, default () {return {}}},
    remember: {type: Boolean, default: false},
    register: {type: Function, default: null},
    allowAddCol: {type: Boolean, default: false},
    noHeaderEdit: {type: Boolean, default: false},
    addColumn: {type: Function, default: null},
    spellcheck: {type: Boolean, default: false},
    newIfBottom: {type: Boolean, default: false},
    validate: {type: Function, default: null},
    localizedLabel: {
      type: Object,
      default () {
        return {
          footerLeft: (top, bottom, total) => `Record ${top} to ${bottom} of ${total}`,
          first: 'First',
          previous: 'Previous',
          next: 'Next',
          last: 'Last',
          footerRight: {
            selected: 'Selected:',
            filtered: 'Filtered:',
            loaded: 'Loaded:'
          },
          processing: 'Processing',
          tableSetting: 'Table Setting',
          exportExcel: 'Export Excel',
          importExcel: 'Import Excel',
          back: 'Back',
          reset: 'Default',
          sortingAndFiltering: 'Sorting And Filtering',
          sortAscending: 'Sort Ascending',
          sortDescending: 'Sort Descending',
          near: '≒ Near',
          exactMatch: '= Exact Match',
          notMatch: '≠ Not Match',
          greaterThan: '&gt; Greater Than',
          greaterThanOrEqualTo: '≥ Greater Than or Equal To',
          lessThan: '&lt; Less Than',
          lessThanOrEqualTo: '≤ Less Than Or Equal To',
          regularExpression: '~ Regular Expression',
          customFilter: 'Custom Filter',
          listFirstNValuesOnly: n => `List first ${n} values only`,
          apply: 'Apply',
          noRecordIsRead: 'No record is read',
          readonlyColumnDetected: 'Readonly column detected',
          columnHasValidationError: (name, err) => `Column ${name} has validation error: ${err}`,
          rowHasValidationError: (row, name, err) => `Row ${row} has validation error for column ${name}: ${err}`,
          noMatchedColumnName: 'No matched column name',
          invalidInputValue: 'Invalid input value',
          missingKeyColumn: 'Missing key column',
          noRecordIndicator: 'No record'
        }
      }
    },
    recordFilter: {
      type: Function,
      default () {
        return true
      }
    }
  },
  data () {
    const pageSize = this.noPaging ? 999999 : 20
    const dataset = {
      version: '1.3',
      tableContent: null,           // Table parent
      systable: null,               // TABLE dom node
      colgroupTr: null,             // colgroup TR dom node
      labelTr: null,                // THEAD label dom node
      filterTr: null,               // THEAD filter dom node
      recordBody: null,             // TBODY dom node
      footer: null,                 // TFOOTER dom node

      pageSize: pageSize,
      pageTop: 0,                   // Current page top pos of [table] array

      selected: {},                 // selected storage in hash, key is the pos of [table] array
      selectedCount: 0,             // selected row count
      prevSelect: -1,               // previous select pos of [table] array
      processing: false,            // current general-purpose processing status

      rowIndex: {},                 // index of the record key to pos of [table] array

      currentRecord: null,          // focusing row content
      currentRowPos: 0,             // focusing array pos of [table] array
      currentColPos: 0,             // focusing pos of column/field
      currentField: null,           // focusing field object
      currentCell: null,
      inputBox: null,
      inputBoxShow: 0,
      inputSquare: null,
      autocompleteInputs: [],
      autocompleteSelect: -1,

      errmsg: {},
      rowerr: {},
      tip: '',
      textTip: '',

      colHash: '',
      fields: [],
      focused: false,
      mousein: false,
      inputBoxChanged: false,

      columnFilter: {},             // set filter storage in hash, key is the column pos

      inputFind: '',
      calCellLeft: 0,
      calCellTop: 0,
      calCellTop2: 29,

      frontdrop: null,              // frontdrop dom node

      sortPos: 0,                   // Sort column position
      sortDir: 0,                   // Sort direction, 1=Ascending, -1=Descending
      redo: [],                     // redo log

      lazyTimeout: {},
      lazyBuffer: {},
      hScroller: {},
      vScroller: {},
      leftMost: 0,

      showDatePicker: false,
      inputDateTime: new Date(),

      table: [],
      filteredValue: [],
      lastFilterTime: '',
      summaryRow: false,
      summary: {},
      showFilteredOnly: true,
      showSelectedOnly: false,

      fillDragActive: false,
      fillDragSource: null,
      fillDragBoundMove: null,
      fillDragBoundUp: null,
      fillDragOverlay: { show: false, style: {} },
      fillDragHoverRow: null,
      fillDragHoverCol: null,

      /** 区域选外框（相对 systable，与 fill 预览同一套坐标） */
      cellRangeOverlay: { show: false, style: {} },
      /** 行号多选且不连续时多块外框（与 cell-range-overlay 同 class） */
      rowSelectOverlaySegments: [],

      /** 矩形区域选：table 行下标 + fields 列下标 */
      cellRange: null,
      cellRangeDrag: null,
      _cellRangeSelectMoveBound: null,
      _cellRangeSelectUpBound: null,
      suppressClearCellRangeOnMove: false,

      /** 行号列拖选多行 */
      rowLabelDrag: null,
      _rowLabelDragMoveBound: null,
      _rowLabelDragUpBound: null,
      _rowLabelDragRaf: null,

      /** 合并 table-content 高频 scroll 到每帧一次 */
      _tableScrollRaf: null
    }
    return dataset
  },
  computed: {
    token () {
      const id = Array.from(document.querySelectorAll('.vue-excel-editor')).indexOf(this.$el)
      return `vue-excel-editor-${id}`
    },
    columnFilterString () {
      Object.keys(this.columnFilter).forEach((key) => {
        if (this.columnFilter[key].trim() === '') delete this.columnFilter[key]
      })
      return JSON.stringify(this.columnFilter)
    },
    pagingTable () {
      return this.table.slice(this.pageTop, this.pageTop + this.pageSize)
    },
    pageBottom () {
      if (this.filteredValue.length === 0) return 0
      else return this.pageTop + this.pageSize > this.table.length ? this.table.length : this.pageTop + this.pageSize
    },
    setting: {
      get () {
        return null
      },
      set (setter) {
        if (setter.fields) {
          // ignore if fields counts are different
          if (setter.fields.length !== this.fields.length) return
          let valid = true
          const newFields = setter.fields.map(local => {
            const current = this.fields.find(f => f.name === local.name)
            if (!current) valid = false
            else {
              if (typeof local.invisible !== 'undefined') current.invisible = local.invisible
              if (typeof local.width !== 'undefined') current.width = local.width
              if (typeof local.label !== 'undefined') current.label = local.label
            }
            return current
          })
          if (valid) {
            this.fields = newFields
            this.$forceUpdate()
          }
        }
      }
    },
    /** 矩形拖选/已选、或选区 overlay 已显示时，活动格只保留填充柄位置，不画第二套蓝框 */
    suppressInputSquareRangeChrome () {
      if (this.inputBoxShow) return false
      if (this.cellRange || this.cellRangeDrag) return true
      if (this.cellRangeOverlay && this.cellRangeOverlay.show) return true
      if (this.rowSelectOverlaySegments && this.rowSelectOverlaySegments.length) return true
      return false
    },
    /** 可编辑纯文本列（无 options 三角/map）：单行 textarea 便于格内垂直居中；含 options 的列勿套（交付物/计划时间等） */
    defectNameTextareaRowBind () {
      if (this.currentField && !this.currentField.readonly && this.currentField.type !== 'date' && !this.currentField.options)
        return { rows: 1 }
      return {}
    }
  },
  watch: {
    value () {
      this.lazy(() => {
        this.refresh()
        if (this.pageTop > this.table.length)
          this.lastPage()
      })
    },
    columnFilterString () {
      this.lastFilterTime = String(new Date().getTime() % 1e8)
      this.processing = true
      setTimeout(() => {
        this.pageTop = 0
        this.refresh()
        this.processing = false
      }, 0)
    },
    fields: {
      handler () {
        this.lazy(() => {
          const setting = this.getSetting()
          if (this.remember) localStorage[window.location.pathname + '.' + this.token] = JSON.stringify(setting)
          this.$emit('setting', setting)
        })
      },
      deep: true
    },
    processing (newVal) {
      if (newVal) {
        const rect = this.$el.children[0].getBoundingClientRect()
        this.frontdrop.style.top = rect.top + 'px'
        this.frontdrop.style.left = rect.left + 'px'
        this.frontdrop.style.height = rect.height + 'px'
        this.frontdrop.style.width = rect.width + 'px'
      }
    },
    pageTop (newVal) {
      this.$emit('page-changed', newVal, newVal + this.pageSize - 1)
      this.$nextTick(() => this.updateCellRangeSelectionOverlay())
    },
    pageSize (newVal) {
      this.$emit('page-changed', this.pageTop, this.pageTop + newVal - 1)
    },
    inputBoxShow (v) {
      if (v)
        this.$nextTick(() => this.syncDefectNameInputBoxLayout())
      else if (this.inputBox) {
        this.inputBox.style.removeProperty('height')
        this.inputBox.style.removeProperty('min-height')
      }
    },
    /**
     * 行选高亮依赖 updateCellRangeSelectionOverlay。仅 watch selectedCount 时，同一事件循环内
     * clearAllSelected(1→0) + selectRecord(0→1) 会合并为「首尾仍是 1」，watcher 不触发，overlay 滞后一次点击。
     */
    selected: {
      handler () {
        this.$nextTick(() => this.updateCellRangeSelectionOverlay())
      },
      deep: true
    }
  },
  beforeDestroy () {
    if (this._tableScrollRaf != null) {
      cancelAnimationFrame(this._tableScrollRaf)
      this._tableScrollRaf = null
    }
    window.removeEventListener('resize', this.winResize)
    window.removeEventListener('paste', this.winPaste)
    window.removeEventListener('keydown', this.winKeydown)
    window.removeEventListener('keyup', this.winKeyup)
    window.removeEventListener('scroll', this.winScroll)
    window.removeEventListener('wheel', this.mousewheel)
    this.fillHandleCleanup()
    this.cellRangeSelectCleanupListeners()
    this.rowLabelDragCleanup()
  },
  mounted () {
    this.editor = this.$refs.editor
    this.tableContent = this.$refs.tableContent
    this.systable = this.$refs.systable
    this.colgroupTr = this.systable.children[0]
    this.labelTr = this.systable.children[1].children[0]
    this.filterTr = this.systable.children[1].children[1]
    this.recordBody = this.systable.children[2]
    this.footer = this.$refs.footer
    this.inputSquare = this.$refs.inputSquare
    this.inputBox = this.$refs.inputBox
    this.frontdrop = this.$refs.frontdrop

    if (this.height)
      this.systable.parentNode.style.height = this.height

    this.reset()
    this.lazy(() => {
      this.labelTr.children[0].style.height = this.labelTr.offsetHeight + 'px'
      this.calCellTop2 = this.labelTr.offsetHeight
      this.refreshPageSize()
      {
        const tc = this.tableContent
        const maxL = Math.max(0, tc.scrollWidth - tc.clientWidth)
        tc.scrollTo(Math.min(Math.max(0, tc.scrollLeft), maxL), tc.scrollTop)
      }
      this.calStickyLeft()
    }, 200)

    if (ResizeObserver) new ResizeObserver(this.winResize).observe(this.tableContent)
    window.addEventListener('resize', this.winResize)
    window.addEventListener('paste', this.winPaste)
    window.addEventListener('keydown', this.winKeydown)
    window.addEventListener('keyup', this.winKeyup)
    window.addEventListener('scroll', this.winScroll)
    window.addEventListener('wheel', this.mousewheel, {passive: false})

    if (this.remember) {
      const saved = localStorage[window.location.pathname + '.' + this.token]
      if (saved) {
        const data = JSON.parse(saved)
        if (data.colHash === this.colHash)
          this.setting = data
      }
    }
  },
  methods: {
    /** 鼠标事件中 target 可能为文本节点等，无 classList；规范化到最近 Element（与宿主页捕获 mousedown 等场景兼容） */
    domEventTargetEl (e) {
      let t = e && e.target
      while (t && t.nodeType !== 1) t = t.parentNode
      return t || null
    },
    /** 单元格内若有子节点（如宿主 v-html 缩略图），事件 target 常为子元素；归一到所在 TD */
    domEventTargetTd (e) {
      let el = this.domEventTargetEl(e)
      while (el && el.tagName !== 'TD') el = el.parentElement
      return el || null
    },
    /** 下拉/日期三角在单元格右侧（background: right 6px center）时的点击热区：右缘向内约 24px（含只读计划列等宿主自定义弹层） */
    tdDropdownHotZoneHit (td, clientX) {
      if (!td || typeof td.getBoundingClientRect !== 'function') return false
      if (!td.classList.contains('select') && !td.classList.contains('datepick')) return false
      const relX = clientX - td.getBoundingClientRect().left
      const w = td.offsetWidth
      return relX >= w - 24
    },
    /** 右下角填充柄：拖动矩形区域，将源单元格值复制到范围内可写格（跳过 key / 只读 / 隐藏 / cellHtml） */
    fillHandleMouseDown (e) {
      if (this.selectedCount > 0) this.clearAllSelected()
      this.cellRangeSelectCleanupListeners()
      e.preventDefault()
      e.stopPropagation()
      /* 先置位以立刻隐藏 rb-square（cleanup 会清 cellRangeDrag，否则 v-show 会短暂为 true） */
      this.fillDragActive = true
      if (!this.focused || this.currentRowPos < 0 || !this.currentField) {
        this.fillDragActive = false
        return
      }
      if (this.inputBoxShow) {
        this.fillDragActive = false
        return
      }
      const field = this.currentField
      if (field.keyField || field.cellHtml) {
        this.fillDragActive = false
        return
      }
      const row = this.pagingTable[this.currentRowPos]
      if (!row) {
        this.fillDragActive = false
        return
      }
      let value = row[field.name]
      if (value !== null && typeof value === 'object') {
        try {
          value = JSON.parse(JSON.stringify(value))
        } catch (err) {
          this.fillDragActive = false
          return
        }
      }
      let fillDragSource = {
        mode: 'single',
        rowPos: this.currentRowPos,
        colPos: this.currentColPos,
        fieldName: field.name,
        value
      }
      const cr = this.cellRange
      const absR = this.pageTop + this.currentRowPos
      if (cr) {
        const ra0 = Math.min(cr.r0, cr.r1)
        const ra1 = Math.max(cr.r0, cr.r1)
        const ca0 = Math.min(cr.c0, cr.c1)
        const ca1 = Math.max(cr.c0, cr.c1)
        const lastPagingRow = ra1 - this.pageTop
        const tryRowStripFromPagingRow = (pRow) => {
          if (pRow < 0 || pRow >= (this.pagingTable && this.pagingTable.length ? this.pagingTable.length : 0)) return null
          const srcRow = this.pagingTable[pRow]
          if (!srcRow) return null
          let ok = true
          const valuesByCol = {}
          for (let c = ca0; c <= ca1; c++) {
            const f = this.fields[c]
            if (!f || f.keyField || f.cellHtml) {
              ok = false
              break
            }
            let v = srcRow[f.name]
            if (v !== null && typeof v === 'object') {
              try {
                v = JSON.parse(JSON.stringify(v))
              } catch (err) {
                ok = false
                break
              }
            }
            valuesByCol[c] = v
          }
          if (!ok || Object.keys(valuesByCol).length === 0) return null
          const anchorF = this.fields[this.currentColPos]
          let anchorVal = anchorF ? srcRow[anchorF.name] : value
          if (anchorVal !== null && typeof anchorVal === 'object') {
            try {
              anchorVal = JSON.parse(JSON.stringify(anchorVal))
            } catch (err) {
              anchorVal = value
            }
          }
          return {
            mode: 'row-strip',
            rowPos: pRow,
            colPos: this.currentColPos,
            fieldName: field.name,
            value: anchorVal,
            cMin: ca0,
            cMax: ca1,
            valuesByCol
          }
        }
        /* 单行多列：源行为该选区行 */
        if (ra0 === ra1 && ra0 === absR && ca1 > ca0) {
          const strip = tryRowStripFromPagingRow(lastPagingRow)
          if (strip) fillDragSource = strip
        } else if (ra1 > ra0 && absR === ra1 && this.currentColPos === ca1) {
          /* 多行选区：以选区最后一行为模板向下填（填充柄须在右下角） */
          const strip = tryRowStripFromPagingRow(lastPagingRow)
          if (strip) fillDragSource = strip
        }
      }
      this.fillDragSource = fillDragSource
      this.fillDragBoundMove = this.fillHandleMouseMove.bind(this)
      this.fillDragBoundUp = this.fillHandleMouseUp.bind(this)
      document.addEventListener('mousemove', this.fillDragBoundMove)
      document.addEventListener('mouseup', this.fillDragBoundUp, true)
      this.fillDragHoverRow = this.currentRowPos
      this.fillDragHoverCol = this.currentColPos
      this.$nextTick(() => {
        this.updateFillDragOverlay(this.fillDragHoverRow, this.fillDragHoverCol)
        this.updateCellRangeSelectionOverlay()
      })
    },
    /** 当前页 tbody 内单元格 DOM：rowPos/colPos 为分页内下标（与 cell-RC 一致） */
    getPagingCellEl (rowPos, colPos) {
      if (!this.recordBody || rowPos < 0 || colPos < 0) return null
      const row = this.recordBody.children[rowPos]
      if (!row || !row.children || colPos + 1 >= row.children.length) return null
      const td = row.children[colPos + 1]
      if (!td || td.tagName !== 'TD' || td.classList.contains('first-col')) return null
      return td
    },
    /** 当前页数据行 TR（与 cell-RC 行下标一致） */
    getPagingRowTrEl (rowPos) {
      if (!this.recordBody || rowPos < 0) return null
      const tr = this.recordBody.children[rowPos]
      if (!tr || tr.tagName !== 'TR') return null
      return tr
    },
    /** 选区 overlay 相对 .table-content（含 scrollTop）：表头区域底边 Y，用于裁切避免盖住标题 */
    getSelectionOverlayHeaderBottomRel () {
      if (!this.tableContent || !this.labelTr) return 0
      const tc = this.tableContent
      const tcR = tc.getBoundingClientRect()
      let bottom = this.labelTr.getBoundingClientRect().bottom
      if (this.filterRow && this.filterTr) {
        const fr = this.filterTr.getBoundingClientRect()
        if (fr.height > 2) bottom = fr.bottom
      }
      return Math.max(0, bottom - tcR.top + tc.scrollTop)
    },
    /** 顶边裁到表头之下；若选区完全在表头背后则返回 null */
    clipSelectionOverlayStyle (style) {
      if (!style) return style
      const top = parseFloat(style.top)
      const h = parseFloat(style.height)
      if (!Number.isFinite(top) || !Number.isFinite(h) || h <= 0) return style
      const hb = this.getSelectionOverlayHeaderBottomRel()
      if (top >= hb) return style
      const cut = hb - top
      const newTop = hb
      const newH = h - cut
      if (newH <= 0) return null
      return Object.assign({}, style, {
        top: newTop + 'px',
        height: newH + 'px'
      })
    },
    /**
     * 选区/填充预览相对 .table-content 定位（须加 scrollLeft/scrollTop），勿用 systable 为参照
     * ——否则横向滚动时左边被裁、底角与格线错位；描边用宿主 border，勿用 outline（overflow 外扩被裁）
     */
    finalizeSelectionOverlayBox (left, top, right, bottom) {
      const tc = this.tableContent
      if (!tc || !Number.isFinite(left) || left === Infinity) return null
      const cr = tc.getBoundingClientRect()
      const relLeft = left - cr.left + tc.scrollLeft
      const relTop = top - cr.top + tc.scrollTop
      const st = {
        left: Math.round(relLeft) + 'px',
        top: Math.round(relTop) + 'px',
        width: Math.max(1, Math.round(right - left)) + 'px',
        height: Math.max(1, Math.round(bottom - top)) + 'px'
      }
      return this.clipSelectionOverlayStyle(st)
    },
    /** 根据源格与目标格（同行或同列）更新绿色预览框，相对 systable 与 input-square 一致 */
    updateFillDragOverlay (targetRow, targetCol) {
      const src = this.fillDragSource
      if (!src || !this.systable) return
      let rMin
      let rMax
      let cMin
      let cMax
      if (src.mode === 'row-strip') {
        const r0s = src.rowPos
        const cLo = src.cMin
        const cHi = src.cMax
        if (typeof targetRow !== 'number' || typeof targetCol !== 'number' || !Number.isFinite(targetRow) || !Number.isFinite(targetCol)) {
          rMin = rMax = r0s
          cMin = cLo
          cMax = cHi
        } else if (targetRow !== r0s) {
          rMin = Math.min(r0s, targetRow)
          rMax = Math.max(r0s, targetRow)
          cMin = cLo
          cMax = cHi
        } else {
          rMin = rMax = r0s
          cMin = cLo
          cMax = cHi
        }
      } else {
        const r0 = src.rowPos
        const c0 = src.colPos
        if (typeof targetRow !== 'number' || typeof targetCol !== 'number' || !Number.isFinite(targetRow) || !Number.isFinite(targetCol)) {
          rMin = rMax = r0
          cMin = cMax = c0
        } else if (targetCol === c0) {
          rMin = Math.min(r0, targetRow)
          rMax = Math.max(r0, targetRow)
          cMin = cMax = c0
        } else if (targetRow === r0) {
          cMin = Math.min(c0, targetCol)
          cMax = Math.max(c0, targetCol)
          rMin = rMax = r0
        } else {
          rMin = rMax = r0
          cMin = cMax = c0
        }
      }
      const els = []
      for (let r = rMin; r <= rMax; r++) {
        for (let c = cMin; c <= cMax; c++) {
          const el = this.getPagingCellEl(r, c)
          if (el) els.push(el)
        }
      }
      if (!els.length) {
        this.$set(this.fillDragOverlay, 'show', false)
        this.$set(this.fillDragOverlay, 'style', {})
        return
      }
      let left = Infinity
      let top = Infinity
      let right = -Infinity
      let bottom = -Infinity
      els.forEach((el) => {
        const br = el.getBoundingClientRect()
        left = Math.min(left, br.left)
        top = Math.min(top, br.top)
        right = Math.max(right, br.right)
        bottom = Math.max(bottom, br.bottom)
      })
      const fst = this.finalizeSelectionOverlayBox(left, top, right, bottom)
      if (!fst) {
        this.$set(this.fillDragOverlay, 'show', false)
        this.$set(this.fillDragOverlay, 'style', {})
        return
      }
      this.$set(this.fillDragOverlay, 'show', true)
      this.$set(this.fillDragOverlay, 'style', fst)
    },
    /** 拖选/已选矩形与行选外框：行选 bbox 为所选各行内全部 TD 的 rect 并集（含序号列），避免高行底边缺线 */
    updateCellRangeSelectionOverlay () {
      if (this.fillDragActive) {
        this.$set(this.cellRangeOverlay, 'show', false)
        this.$set(this.cellRangeOverlay, 'style', {})
        this.rowSelectOverlaySegments = []
        return
      }
      const nRow = this.pagingTable && this.pagingTable.length ? this.pagingTable.length : 0
      if (nRow <= 0 || !this.systable) {
        this.$set(this.cellRangeOverlay, 'show', false)
        this.$set(this.cellRangeOverlay, 'style', {})
        this.rowSelectOverlaySegments = []
        return
      }
      const nCol = this.fields ? this.fields.length : 0
      if (nCol <= 0) {
        this.$set(this.cellRangeOverlay, 'show', false)
        this.$set(this.cellRangeOverlay, 'style', {})
        this.rowSelectOverlaySegments = []
        return
      }

      this.rowSelectOverlaySegments = []
      const els = []
      if (this.cellRangeDrag) {
        let rMin = Math.min(this.cellRangeDrag.pr0, this.cellRangeDrag.pr1)
        let rMax = Math.max(this.cellRangeDrag.pr0, this.cellRangeDrag.pr1)
        let cMin = Math.min(this.cellRangeDrag.pc0, this.cellRangeDrag.pc1)
        let cMax = Math.max(this.cellRangeDrag.pc0, this.cellRangeDrag.pc1)
        rMin = Math.max(0, Math.min(rMin, nRow - 1))
        rMax = Math.max(0, Math.min(rMax, nRow - 1))
        if (rMin > rMax) {
          const t = rMin
          rMin = rMax
          rMax = t
        }
        cMin = Math.max(0, Math.min(cMin, nCol - 1))
        cMax = Math.max(0, Math.min(cMax, nCol - 1))
        if (cMin > cMax) {
          const t = cMin
          cMin = cMax
          cMax = t
        }
        for (let r = rMin; r <= rMax; r++) {
          for (let c = cMin; c <= cMax; c++) {
            const el = this.getPagingCellEl(r, c)
            if (!el) continue
            if (this.fields[c] && this.fields[c].invisible) continue
            const br = el.getBoundingClientRect()
            if (br.width < 1 || br.height < 1) continue
            els.push(el)
          }
        }
      } else if (this.cellRange) {
        const top = this.pageTop
        let rMin = Math.min(this.cellRange.r0, this.cellRange.r1) - top
        let rMax = Math.max(this.cellRange.r0, this.cellRange.r1) - top
        let cMin = Math.min(this.cellRange.c0, this.cellRange.c1)
        let cMax = Math.max(this.cellRange.c0, this.cellRange.c1)
        rMin = Math.max(0, Math.min(rMin, nRow - 1))
        rMax = Math.max(0, Math.min(rMax, nRow - 1))
        if (rMin > rMax) {
          const t = rMin
          rMin = rMax
          rMax = t
        }
        cMin = Math.max(0, Math.min(cMin, nCol - 1))
        cMax = Math.max(0, Math.min(cMax, nCol - 1))
        if (cMin > cMax) {
          const t = cMin
          cMin = cMax
          cMax = t
        }
        for (let r = rMin; r <= rMax; r++) {
          for (let c = cMin; c <= cMax; c++) {
            const el = this.getPagingCellEl(r, c)
            if (!el) continue
            if (this.fields[c] && this.fields[c].invisible) continue
            const br = el.getBoundingClientRect()
            if (br.width < 1 || br.height < 1) continue
            els.push(el)
          }
        }
      } else if (this.selectedCount > 0) {
        const pageTop = this.pageTop
        const pageEnd = pageTop + nRow
        const vis = Object.keys(this.selected)
          .map(Number)
          .filter((abs) => Number.isFinite(abs) && abs >= pageTop && abs < pageEnd)
          .sort((a, b) => a - b)
        if (!vis.length) {
          this.$set(this.cellRangeOverlay, 'show', false)
          this.$set(this.cellRangeOverlay, 'style', {})
          this.rowSelectOverlaySegments = []
          return
        }
        const runs = []
        let cur = []
        vis.forEach((abs) => {
          if (!cur.length || abs === cur[cur.length - 1] + 1) cur.push(abs)
          else {
            runs.push(cur)
            cur = [abs]
          }
        })
        if (cur.length) runs.push(cur)
        /* 必须合并行内每个 TD 的 bbox：TR 的 outline/整行 getBoundingClientRect 在多行高格时底边会短一截 */
        const bboxStyleForAbsRowRun = (absRun) => {
          if (!absRun.length) return null
          let left = Infinity
          let top = Infinity
          let right = -Infinity
          let bottom = -Infinity
          absRun.forEach((abs) => {
            const r = abs - pageTop
            const tr = this.getPagingRowTrEl(r)
            if (!tr || !tr.children) return
            for (let i = 0; i < tr.children.length; i++) {
              const cell = tr.children[i]
              if (!cell || cell.tagName !== 'TD') continue
              const br = cell.getBoundingClientRect()
              if (br.width < 1 || br.height < 1) continue
              left = Math.min(left, br.left)
              top = Math.min(top, br.top)
              right = Math.max(right, br.right)
              bottom = Math.max(bottom, br.bottom)
            }
          })
          if (!Number.isFinite(left) || left === Infinity) return null
          const style = this.finalizeSelectionOverlayBox(left, top, right, bottom)
          if (!style) return null
          return { show: true, style }
        }
        if (runs.length > 1) {
          const segs = runs.map((run) => bboxStyleForAbsRowRun(run)).filter(Boolean)
          this.$set(this.cellRangeOverlay, 'show', false)
          this.$set(this.cellRangeOverlay, 'style', {})
          this.rowSelectOverlaySegments = segs.length ? segs : []
          return
        }
        const one = bboxStyleForAbsRowRun(runs[0])
        if (!one) {
          this.$set(this.cellRangeOverlay, 'show', false)
          this.$set(this.cellRangeOverlay, 'style', {})
          this.rowSelectOverlaySegments = []
          return
        }
        this.$set(this.cellRangeOverlay, 'show', true)
        this.$set(this.cellRangeOverlay, 'style', one.style)
        this.rowSelectOverlaySegments = []
        return
      } else {
        this.$set(this.cellRangeOverlay, 'show', false)
        this.$set(this.cellRangeOverlay, 'style', {})
        this.rowSelectOverlaySegments = []
        return
      }
      if (!els.length) {
        this.$set(this.cellRangeOverlay, 'show', false)
        this.$set(this.cellRangeOverlay, 'style', {})
        this.rowSelectOverlaySegments = []
        return
      }
      let left = Infinity
      let top = Infinity
      let right = -Infinity
      let bottom = -Infinity
      els.forEach((el) => {
        const br = el.getBoundingClientRect()
        left = Math.min(left, br.left)
        top = Math.min(top, br.top)
        right = Math.max(right, br.right)
        bottom = Math.max(bottom, br.bottom)
      })
      const cst = this.finalizeSelectionOverlayBox(left, top, right, bottom)
      if (!cst) {
        this.$set(this.cellRangeOverlay, 'show', false)
        this.$set(this.cellRangeOverlay, 'style', {})
        this.rowSelectOverlaySegments = []
        return
      }
      this.$set(this.cellRangeOverlay, 'show', true)
      this.$set(this.cellRangeOverlay, 'style', cst)
    },
    fillHandleMouseMove (e) {
      if (!this.fillDragActive || !this.fillDragSource) return
      const raw = document.elementFromPoint(e.clientX, e.clientY)
      const td = raw && this.domEventTargetTd({ target: raw })
      if (td && td.parentNode && td.parentNode.parentNode && td.parentNode.parentNode.tagName === 'TBODY' && !td.classList.contains('first-col')) {
        const rc = td.getAttribute('cell-RC')
        const m = rc && /^(\d+)-/.exec(rc)
        if (m) {
          const rowIdx = parseInt(m[1], 10)
          const fieldName = rc.slice(m[0].length)
          const c1 = this.fields.findIndex(f => f.name === fieldName)
          if (Number.isFinite(rowIdx) && c1 >= 0) {
            this.fillDragHoverRow = rowIdx
            this.fillDragHoverCol = c1
          }
        }
      }
      this.updateFillDragOverlay(this.fillDragHoverRow, this.fillDragHoverCol)
    },
    fillHandleMouseUp (e) {
      if (!this.fillDragActive || !this.fillDragSource) {
        this.fillHandleCleanup()
        return
      }
      const src = this.fillDragSource
      this.fillHandleCleanup()
      const raw = document.elementFromPoint(e.clientX, e.clientY)
      const td = raw && this.domEventTargetTd({ target: raw })
      if (!td || td.tagName !== 'TD' || td.classList.contains('first-col')) return
      if (td.parentNode.parentNode.tagName !== 'TBODY') return
      const rc = td.getAttribute('cell-RC')
      const m = rc && /^(\d+)-/.exec(rc)
      if (!m) return
      const rowIdx = parseInt(m[1], 10)
      const fieldName = rc.slice(m[0].length)
      if (!Number.isFinite(rowIdx) || !fieldName) return
      const c1 = this.fields.findIndex(f => f.name === fieldName)
      if (c1 < 0) return
      const r0 = src.rowPos
      const c0 = src.colPos
      const r1 = rowIdx
      if (src.mode === 'row-strip') {
        const cLo = src.cMin
        const cHi = src.cMax
        if (r1 !== r0) {
          const rMin = Math.min(r0, r1)
          const rMax = Math.max(r0, r1)
          const vbc = src.valuesByCol
          for (let r = rMin; r <= rMax; r++) {
            if (r === r0) continue
            if (!this.pagingTable[r]) continue
            for (let c = cLo; c <= cHi; c++) {
              const f = this.fields[c]
              if (!f || f.readonly || f.keyField || f.invisible || f.cellHtml) continue
              const copyVal = vbc[c]
              if (copyVal === undefined) continue
              try {
                this.updateCell(this.pageTop + r, f, copyVal)
              } catch (err) {}
            }
          }
        }
        return
      }
      if (r0 === r1 && c0 === c1) return
      /* 仅同行横向或同列纵向，避免斜向误填多列 */
      if (r1 !== r0 && c1 !== c0) return
      const copyVal = src.value
      if (c1 === c0) {
        const rMin = Math.min(r0, r1)
        const rMax = Math.max(r0, r1)
        const f = this.fields[c0]
        if (!f || f.readonly || f.keyField || f.invisible || f.cellHtml) return
        for (let r = rMin; r <= rMax; r++) {
          if (r === r0) continue
          if (!this.pagingTable[r]) continue
          try {
            this.updateCell(this.pageTop + r, f, copyVal)
          } catch (err) {}
        }
      } else if (r1 === r0) {
        const cMin = Math.min(c0, c1)
        const cMax = Math.max(c0, c1)
        for (let c = cMin; c <= cMax; c++) {
          if (c === c0) continue
          const f = this.fields[c]
          if (!f || f.readonly || f.keyField || f.invisible || f.cellHtml) continue
          if (!this.pagingTable[r0]) continue
          try {
            this.updateCell(this.pageTop + r0, f, copyVal)
          } catch (err) {}
        }
      }
    },
    /**
     * 矩形选区批量清空（Del / Backspace）。
     * 上游组件无原生区域多选，仅支持当前格；与填充柄一致跳过 key / 只读 / 隐藏 / cellHtml。
     */
    clearCellRangeContents () {
      if (!this.cellRange || this.fillDragActive) return
      const r0 = Math.min(this.cellRange.r0, this.cellRange.r1)
      const r1 = Math.max(this.cellRange.r0, this.cellRange.r1)
      const c0 = Math.min(this.cellRange.c0, this.cellRange.c1)
      const c1 = Math.max(this.cellRange.c0, this.cellRange.c1)
      for (let absR = r0; absR <= r1; absR++) {
        if (!this.table[absR]) continue
        for (let c = c0; c <= c1; c++) {
          const field = this.fields[c]
          if (!field || field.readonly || field.keyField || field.invisible || field.cellHtml) continue
          try {
            const emptyVal = field.toValue('')
            this.updateCell(absR, field, emptyVal)
          } catch (err) {}
        }
      }
      this.inputBoxChanged = false
      this.$nextTick(() => {
        if (this.currentCell && !this.inputBoxShow) {
          this.inputBox.value = this.currentCell.textContent
        }
        this.updateCellRangeSelectionOverlay()
      })
    },
    cellRangeSelectCleanupListeners () {
      if (this._cellRangeSelectMoveBound) {
        document.removeEventListener('mousemove', this._cellRangeSelectMoveBound, true)
        document.removeEventListener('mouseup', this._cellRangeSelectUpBound, true)
        this._cellRangeSelectMoveBound = null
        this._cellRangeSelectUpBound = null
      }
      this.cellRangeDrag = null
      this.$nextTick(() => this.updateCellRangeSelectionOverlay())
    },
    cellRangeSelectArm (rowPos, colPos) {
      this.cellRangeSelectCleanupListeners()
      this.cellRange = null
      this.cellRangeDrag = {
        pr0: rowPos,
        pc0: colPos,
        pr1: rowPos,
        pc1: colPos
      }
      this._cellRangeSelectMoveBound = this.cellRangeSelectMove.bind(this)
      this._cellRangeSelectUpBound = this.cellRangeSelectUp.bind(this)
      document.addEventListener('mousemove', this._cellRangeSelectMoveBound, true)
      document.addEventListener('mouseup', this._cellRangeSelectUpBound, true)
      this.$nextTick(() => this.updateCellRangeSelectionOverlay())
    },
    cellRangeSelectMove (e) {
      if (!this.cellRangeDrag || this.fillDragActive) return
      /* 不设 e.buttons 门槛：部分环境下拖选时 buttons 为 0，会导致矩形不更新 */
      const raw = document.elementFromPoint(e.clientX, e.clientY)
      const td = raw && this.domEventTargetTd({ target: raw })
      if (!td || td.parentNode.parentNode.tagName !== 'TBODY' || td.classList.contains('first-col')) return
      const rc = td.getAttribute('cell-RC')
      const m = rc && /^(\d+)-/.exec(rc)
      if (!m) return
      const pr1 = parseInt(m[1], 10)
      const fieldName = rc.slice(m[0].length)
      const pc1 = this.fields.findIndex(f => f.name === fieldName)
      if (pc1 < 0) return
      const d = this.cellRangeDrag
      if (d.pr1 === pr1 && d.pc1 === pc1) return
      this.$set(this.cellRangeDrag, 'pr1', pr1)
      this.$set(this.cellRangeDrag, 'pc1', pc1)
      this.$nextTick(() => this.updateCellRangeSelectionOverlay())
    },
    cellRangeSelectUp (e) {
      if (this._cellRangeSelectMoveBound) {
        document.removeEventListener('mousemove', this._cellRangeSelectMoveBound, true)
        document.removeEventListener('mouseup', this._cellRangeSelectUpBound, true)
        this._cellRangeSelectMoveBound = null
        this._cellRangeSelectUpBound = null
      }
      if (!this.cellRangeDrag) return
      const d = this.cellRangeDrag
      this.cellRangeDrag = null
      let pr1 = d.pr1
      let pc1 = d.pc1
      const raw = document.elementFromPoint(e.clientX, e.clientY)
      const td = raw && this.domEventTargetTd({ target: raw })
      if (td && td.parentNode.parentNode.tagName === 'TBODY' && !td.classList.contains('first-col')) {
        const rc = td.getAttribute('cell-RC')
        const m = rc && /^(\d+)-/.exec(rc)
        if (m) {
          const ridx = parseInt(m[1], 10)
          const fname = rc.slice(m[0].length)
          const cidx = this.fields.findIndex(f => f.name === fname)
          if (cidx >= 0) {
            pr1 = ridx
            pc1 = cidx
          }
        }
      }
      const r0a = this.pageTop + Math.min(d.pr0, pr1)
      const r1a = this.pageTop + Math.max(d.pr0, pr1)
      const c0a = Math.min(d.pc0, pc1)
      const c1a = Math.max(d.pc0, pc1)
      if (this.fillDragActive) {
        this.suppressClearCellRangeOnMove = true
        try {
          this.moveInputSquare(pr1, pc1)
        } finally {
          this.suppressClearCellRangeOnMove = false
        }
        this.$nextTick(() => this.updateCellRangeSelectionOverlay())
        return
      }
      if (r0a === r1a && c0a === c1a) {
        this.cellRange = null
      } else {
        this.cellRange = { r0: r0a, r1: r1a, c0: c0a, c1: c1a }
        const fieldSlice = []
        for (let c = c0a; c <= c1a; c++) {
          if (this.fields[c]) fieldSlice.push(this.fields[c].name)
        }
        this.$emit('cell-range-select', {
          r0: r0a,
          r1: r1a,
          c0: c0a,
          c1: c1a,
          fieldNames: fieldSlice
        })
      }
      /* 填充柄在 input-square 右下角：多格选区应对齐选区右下角（与 Excel 一致），勿用松开鼠标的对角格 */
      let focusRowPos = pr1
      let focusColPos = pc1
      if (this.cellRange) {
        focusRowPos = r1a - this.pageTop
        focusColPos = c1a
      }
      this.suppressClearCellRangeOnMove = true
      try {
        this.moveInputSquare(focusRowPos, focusColPos)
      } finally {
        this.suppressClearCellRangeOnMove = false
      }
      this.$nextTick(() => this.updateCellRangeSelectionOverlay())
    },

    fillHandleCleanup () {
      this.fillDragActive = false
      this.fillDragSource = null
      this.fillDragHoverRow = null
      this.fillDragHoverCol = null
      this.$set(this.fillDragOverlay, 'show', false)
      this.$set(this.fillDragOverlay, 'style', {})
      if (this.fillDragBoundMove) {
        document.removeEventListener('mousemove', this.fillDragBoundMove)
        this.fillDragBoundMove = null
      }
      if (this.fillDragBoundUp) {
        document.removeEventListener('mouseup', this.fillDragBoundUp, true)
        this.fillDragBoundUp = null
      }
      this.$nextTick(() => this.updateCellRangeSelectionOverlay())
    },
    componentTabInto (e) {
      if (e.keyCode === 9) {
        if (!this.moveInputSquare(this.currentRowPos, this.currentColPos))
          this.moveInputSquare(0, 0)
      }
    },
    reset () {
      this.errmsg = {}
      this.redo = []
      this.showFilteredOnly = true
      this.showSelectedOnly = false
      this.columnFilter = {}
      this.sortPos = 0
      this.sortDir = 0
      this.inputFind = ''
      this.pageTop = 0
      this.selected = {}
      this.selectedCount = 0
      this.prevSelect = -1
      this.processing = false
      this.rowIndex = {}
      this.cellRange = null
      this.cellRangeSelectCleanupListeners()
      this.refresh()
    },
    toggleSelectView (e) {
      if (e) e.stopPropagation()
      this.showSelectedOnly = !this.showSelectedOnly
      this.firstPage()
      return this.refresh()
    },
    toggleFilterView (e) {
      if (e) e.stopPropagation()
      this.showFilteredOnly = !this.showFilteredOnly
      return this.refresh()
    },
    resetColumn () {
      this.fields = []
      //this.$slots.default.forEach(col => col.componentInstance? col.componentInstance.init() : 0)
      this.tableContent.scrollTo(0, this.tableContent.scrollTop)
      this.calStickyLeft()
    },
    registerColumn (field) {
      let pos = this.fields.findIndex(item => item.pos > field.pos)
      if (pos === -1) pos = this.fields.length
      this.fields.splice(pos, 0, field)
      if (this.register) this.register(field, pos)
      if (field.register) field.register(field, pos)
      if (field.summary) this.summaryRow = true
      this.colHash = this.hashCode(this.version + JSON.stringify(this.fields))
    },
    insertColumn (pos) {
      const colname = 'COL-' + Math.random().toString().slice(2,6)
      let colDef = {
        name: colname,
        label: colname,
        type: 'string',
        width: '100px',

        validate: null,
        change: null,
        link: null,
        sort: null,

        keyField: false,
        sticky: false,
        // tabStop: true,
        allowKeys: null,
        mandatory: false,
        lengthLimit: 0,

        autocomplete: this.autocomplete,
        textTransform: null,
        initStyle: 'left',
        invisible: false,
        readonly: this.readonly,
        pos: 0,
        options: null,
        summary: null,
        toValue: t => t,
        toText: t => t,
        register: null
      }
      if (this.addColumn) colDef = this.addColumn(colDef)
      this.newColumn(colDef, pos)
    },
    newColumn (field, pos) {
      this.fields.splice(pos, 0, field)
      if (this.register) this.register(field, pos)
      if (field.register) field.register(field, pos)
      if (field.summary) this.summaryRow = true
      this.colHash = this.hashCode(this.version + JSON.stringify(this.fields))
    },
    autoRegisterAllColumns (rows) {
      // If no field is defined, this function will help to create all fields based on provided row sample argument
      const widths = rows.slice(0, 25)
        .reduce((t, v) => Object.keys(v).map((s, i) => !t || v[s].length > t[i]? v[s].length: t[i]), 0)
        .map(v => Math.min(Math.max(v * 8.2, 55), 250))

      Object.keys(rows[0]).forEach((col, i) => {
        if (col === '$id') return
        this.registerColumn({
          name: col,
          label: col,
          type: widths[i]? 'string': 'number',
          width: (widths[i]? widths[i]: 75) + 'px',
          validate: null,
          change: null,
          link: null,
          keyField: false,
          sticky: false,
          tabStop: true,
          allowKeys: null,
          mandatory: false,
          lengthLimit: 0,
          autocomplete: this.autocomplete,
          initStyle: {textAlign: widths[i]? 'left': 'right'},
          invisible: false,
          readonly: this.readonly,
          pos: 0,
          options: null,
          summary: null,
          sort: null,
          toValue: t => t,
          toText: t => t,
          register: null
        })
      })
    },
    refresh () {
      // this.pageTop = 0
      this.prevSelect = -1
      if (this.fields.length === 0 && this.value.length && Object.keys(this.value[0])) {
        this.autoRegisterAllColumns(this.value)
      }
      this.calTable()
      this.refreshPageSize()
    },
    calTable () {
      this.textTip = ''
      // add unique key to each row if no key is provided
      let seed = String(new Date().getTime() % 1e8)
      this.value.forEach((rec,i) => {
        if (!rec.$id) rec.$id = seed + '-' + ('000000' + i).slice(-7)
      })

      if (this.showFilteredOnly === false) {
        this.table = this.value
      }
      else {
        const filterColumnList = Object.keys(this.columnFilter)
        const filter = {}
        filterColumnList.forEach((k) => {
          switch (true) {
            case this.columnFilter[k].startsWith('<='):
              filter[k] = {type: 1, value: this.columnFilter[k].slice(2).trim().toUpperCase()}
              if (this.fields[k].type === 'number') filter[k].value = Number(filter[k].value)
              break
            case this.columnFilter[k].startsWith('<>'):
              filter[k] = {type: 9, value: this.columnFilter[k].slice(2).trim().toUpperCase()}
              break
            case this.columnFilter[k].startsWith('<'):
              filter[k] = {type: 2, value: this.columnFilter[k].slice(1).trim().toUpperCase()}
              if (this.fields[k].type === 'number') filter[k].value = Number(filter[k].value)
              break
            case this.columnFilter[k].startsWith('>='):
              filter[k] = {type: 3, value: this.columnFilter[k].slice(2).trim().toUpperCase()}
              if (this.fields[k].type === 'number') filter[k].value = Number(filter[k].value)
              break
            case this.columnFilter[k].startsWith('>'):
              filter[k] = {type: 4, value: this.columnFilter[k].slice(1).trim().toUpperCase()}
              if (this.fields[k].type === 'number') filter[k].value = Number(filter[k].value)
              break
            case this.columnFilter[k].startsWith('='):
              filter[k] = {type: 0, value: this.columnFilter[k].slice(1).trim().toUpperCase()}
              break
            case this.columnFilter[k].startsWith('*') && this.columnFilter[k].endsWith('*'):
              filter[k] = {type: 5, value: this.columnFilter[k].slice(1).slice(0, -1).trim().toUpperCase()}
              break
            case this.columnFilter[k].startsWith('*') && !this.columnFilter[k].slice(1).includes('*'):
              filter[k] = {type: 6, value: this.columnFilter[k].slice(1).trim().toUpperCase()}
              break
            case this.columnFilter[k].startsWith('~'):
              filter[k] = {type: 8, value: this.columnFilter[k].slice(1).trim()}
              break
            case this.columnFilter[k].endsWith('*') && !this.columnFilter[k].slice(0, -1).includes('*'):
              filter[k] = {type: 7, value: this.columnFilter[k].slice(0, -1).trim().toUpperCase()}
              break
            case this.columnFilter[k].includes('*') || this.columnFilter[k].includes('?'):
              filter[k] = {type: 8, value: '^' + this.columnFilter[k].replace(/\*/g, '.*').replace(/\?/g, '.').trim() + '$'}
              break
            default:
              filter[k] = {type: 5, value: this.columnFilter[k].trim().toUpperCase()}
              break
          }
        })
        this.filteredValue = this.value.filter(record => this.recordFilter(record))
        if (filterColumnList.length === 0)
          this.table = this.filteredValue
        else {
          this.table = this.filteredValue.filter((record) => {

            // Record is created after the filter time
            if (record.$id > this.lastFilterTime) return true

            // Assume new record contains § in any of the key fields
            /*
            const isNew = this.fields.filter((field) => {
              return field.keyField && record[field.name] && record[field.name].startsWith('§')
            }).length > 0
            if (isNew) return true // Always show new record in filter mode
            */

            const content = {}
            filterColumnList.forEach((k) => {
              const val = record[this.fields[k].name]
              if (this.fields[k].type === 'number' && filter[k].type <= 4)
                content[k] = val
              else
                content[k] = typeof val === 'undefined' || val === null ? '' : String(val).toUpperCase()
            })

            for (let i = 0; i < filterColumnList.length; i++) {
              const k = filterColumnList[i]
              switch (filter[k].type) {
                case 0:
                  if (`${content[k]}` !== `${filter[k].value}`) return false
                  break
                case 1:
                  if (filter[k].value < content[k]) return false
                  break
                case 2:
                  if (filter[k].value <= content[k]) return false
                  break
                case 3:
                  if (filter[k].value > content[k]) return false
                  break
                case 4:
                  if (filter[k].value >= content[k]) return false
                  break
                case 5:
                  if (!content[k].includes(filter[k].value)) return false
                  break
                case 6:
                  if (!content[k].endsWith(filter[k].value)) return false
                  break
                case 7:
                  if (!content[k].startsWith(filter[k].value)) return false
                  break
                case 8:
                  if (!new RegExp(filter[k].value, 'i').test(content[k])) return false
                  break
                case 9:
                  if (`${content[k]}` === `${filter[k].value}`) return false
                  break
              }
            }
            return true
          })
        }
      }

      this.reviseSelectedAfterTableChange()
      if (this.showSelectedOnly) {
        this.table = this.table.filter((rec, i) => this.selected[i])
        this.reviseSelectedAfterTableChange()
      }
      this.calSummary()
    },
    calStickyLeft () {
      let left = 0, n = 0
      this.leftMost = -1
      let changed = false
      // this.tableContent.scrollTo(0, this.tableContent.scrollTop)
      Array.from(this.labelTr.children).forEach(th => {
        left += th.offsetWidth
        const field = this.fields[n++]
        if (field) {
          if (field.sticky) {
            const nextLeft = left + 'px'
            if (field.left !== nextLeft) {
              field.left = nextLeft
              changed = true
            }
            this.leftMost = -1 // Reset the leftMost, so it will equal to the next non-sticky col
          }
          else if (this.leftMost === -1) this.leftMost = left
        }
      })
      if (changed) this.$forceUpdate()
    },
    renderColumnCellStyle (field) {
      let result = field.initStyle
      if (field.readonly) result = Object.assign(result, this.readonlyStyle)
      if (field.left) result = Object.assign(result, {left: field.left})
      return result
    },
    localeDate (d) {
      if (typeof d === 'undefined') d = new Date()
      const pad = n => n < 10 ? '0'+n : n;
      return d.getFullYear() + '-'
            + pad(d.getMonth() + 1) + '-'
            + pad(d.getDate()) + ' '
            + pad(d.getHours()) + ':'
            + pad(d.getMinutes()) + ':'
            + pad(d.getSeconds())
     },
    calSummary (name) {
      this.fields.forEach(field => {
        if (!field.summary) return
        const i = field.name
        if (name && name !== i) return
        let result = ''
        const currentTick = new Date().getTime()
        const currentDateTimeSec = this.localeDate()
        const currentDateTime = currentDateTimeSec.slice(0, 19)
        const currentDate = currentDateTimeSec.slice(0, 10)
        switch(field.summary) {
          case 'sum':
            result = this.table.reduce((a, b) => (a + Number(b[i] ? b[i] : 0)), 0)
            result = Number(Math.round(result + 'e+5') + 'e-5')  // solve the infinite .9 issue of javascript
            break
          case 'avg':
            result = this.table.reduce((a, b) => (a + Number(b[i] ? b[i] : 0)), 0) / this.table.length
            result = Number(Math.round(result + 'e+5') + 'e-5')  // solve the infinite .9 issue of javascript
            break
          case 'max':
            result = this.table.reduce((a, b) => (a > b[i] ? a : b[i]), Number.MIN_VALUE)
            break
          case 'min':
            result = this.table.reduce((a, b) => (a < b[i] ? a : b[i]), Number.MAX_VALUE)
            break
          case 'count':
            switch(field.type) {
              case 'checkYN':
                result = this.table.reduce((a, b) => (a + (b[i] === 'Y' ? 1 : 0)), 0)
                break
              case 'check10':
                result = this.table.reduce((a, b) => (a + (b[i] === '1' ? 1 : 0)), 0)
                break
              case 'checkTF':
                result = this.table.reduce((a, b) => (a + (b[i] === 'T' ? 1 : 0)), 0)
                break
              case 'date':
                result = this.table.reduce((a, b) => (a + (b[i] >= currentDate ? 1 : 0)), 0)
                this.summary[i] = result
                return
              case 'datetime':
                result = this.table.reduce((a, b) => (a + (b[i] >= currentDateTime ? 1 : 0)), 0)
                this.summary[i] = result
                return
              case 'datetimesec':
                result = this.table.reduce((a, b) => (a + (b[i] >= currentDateTimeSec ? 1 : 0)), 0)
                this.summary[i] = result
                return
              case 'datetick':
              case 'datetimetick':
              case 'datetimesectick':
                result = this.table.reduce((a, b) => (a + (b[i] >= currentTick ? 1 : 0)), 0)
                this.summary[i] = result
                return
              default:
                result = this.table.reduce((a, b) => (a + (b[i]? 1 : 0)), 0)
                break
            }
            break
        }
        if (field.type === 'number' && isNaN(result)) return
        this.summary[i] = field.toText(result)
      })
    },
    getKeys (rec) {
      if (!rec) rec = this.currentRecord
      const key = this.fields.filter(field => field.keyField).map(field => rec[field.name])
      if (key.length && key.join() !== '') return key
      return [rec.$id]
    },
    getFieldByName (name) {
      return this.fields.find(f => f.name === name)
    },
    getFieldByLabel (label) {
      return this.fields.find(f => f.label === label)
    },

    /* *** Customization **************************************************************************************
     */
    setFilter(name, filterText) {
      const ref = this.$refs[`filter-${name}`][0]
      ref.$el.textContent = filterText
      ref.$emit('input', filterText)
    },

    clearFilter(name) {
      if (!name) this.columnFilter = {}
      else this.setFilter(name, '')
    },

    columnSuppress () {
      if (this.table.length === 0) return
      const cols = {}
      this.table.forEach((row) => {
        Object.keys(row).forEach((field) => {
          if (row[field]) cols[field] = 1
        })
      })
      const showCols = Object.keys(cols)
      this.fields.forEach((field) => {
        if (!showCols.includes(field.name))
          field.invisible = true
      })
      // this.refresh()
    },

    /* Still evaluating */
    columnAutoWidth (name) {
      if (this.table.length === 0) return
      let doFields = this.fields
      if (name) doFields = [this.fields.find(f => f.name === name)]

      const cols = {}
      this.table.forEach((row) => {
        doFields.forEach((field) => {
          if (row[field.name] && (!cols[field.name] || cols[field.name] < row[field.name].length))
            cols[field.name] = row[field.name].length
        })
      })
      doFields.forEach((field) => {
        let width = cols[field.name] * 12
        if (width > 450) width = 450
        field.width = width + 'px'
      })
      // this.refresh()
    },

    columnFillWidth () {
      if (this.table.length === 0) return
      if (!this.editor) return
      const doFields = this.fields.filter(f => f.autoFillWidth)
      const count = doFields.length
      if (!count) return

      const fullWidth = this.editor.getBoundingClientRect().width
      const viewWidth = this.fields.filter(f => !f.invisible).reduce((c, f) => c - -f.width.replace(/px$/, ''), 0)
        + (this.noNumCol ? 0 : 40)
      const fillWidth = viewWidth - fullWidth + 2
      if (fillWidth)
        doFields.forEach(f => f.width = (f.width.replace(/px$/, '') - fillWidth / count) + 'px')
    },

    /* *** Date Picker *********************************************************************************
     */
    showDatePickerDiv () {
      if (!this.$refs.dpContainer) return
      const cellRect = this.currentCell.getBoundingClientRect()
      this.$refs.dpContainer.style.left = (cellRect.left) + 'px'
      this.$refs.dpContainer.style.top = (cellRect.bottom) + 'px'
      this.inputDateTime = this.currentCell.textContent
      this.showDatePicker = true
      this.lazy(() => {
        if (!this.$refs.dpContainer) return
        const r = this.$refs.dpContainer.getBoundingClientRect()
        if (r.bottom > window.innerHeight)
          this.$refs.dpContainer.style.top = (cellRect.top - r.height) + 'px'
        if (r.right > window.innerWidth)
          this.$refs.dpContainer.style.left = (window.innerWidth - r.width) + 'px'
      })
    },
    datepickerClick () {
      this.inputBox.value = this.inputDateTime
      this.inputBoxShow = 0
      this.inputCellWrite(this.inputDateTime)
      this.showDatePicker = false
      this.focused = true
    },

    /* *** Vertical Scrollbar *********************************************************************************
     */
    calVScroll () {
      let d = this.labelTr.getBoundingClientRect().height
      if (this.filterRow) d += 29
      const top = d
      if (!this.noFooter) d += 25
      if (this.summaryRow) d += 27
      const fullHeight = this.$el.getBoundingClientRect().height
      const height = fullHeight - d
      const ratio = this.table.length ? height / (this.table.length * 24) : 1
      const buttonHeight = Math.max(24, height * ratio)
      const denom = this.table.length * 24 - height
      const prop = denom > 0 ? (this.tableContent.scrollTop + this.pageTop * 24) / denom : 0
      const buttonTop = (height - buttonHeight) * prop
      this.$set(this.vScroller, 'top', top)
      this.$set(this.vScroller, 'height', height)
      this.$set(this.vScroller, 'buttonHeight', buttonHeight)
      this.$set(this.vScroller, 'buttonTop', Number.isFinite(buttonTop) ? buttonTop : 0)
    },
    vsMouseDown (e) {
      e.stopPropagation()
      const pos = e.offsetY - this.vScroller.buttonHeight / 2
      let ratio = Math.max(0, pos)
      ratio = Math.min(ratio, this.vScroller.height - this.vScroller.buttonHeight)
      ratio = ratio / (this.vScroller.height - this.vScroller.buttonHeight)
      if (this.noPaging)
        this.tableContent.scrollTo(this.tableContent.scrollLeft, this.table.length * 24 * ratio)
      else {
        this.vScroller.buttonTop = ratio * (this.vScroller.height - this.vScroller.buttonHeight)
        this.$refs.vScrollButton.style.marginTop = this.vScroller.buttonTop + 'px'
        this.pageTop = Math.round((this.table.length - this.pageSize) * ratio)
      }
    },
    vsbMouseDown (e) {
      e.stopPropagation()
      if (!this.$refs.vScrollButton) return
      if (!this.vScroller.mouseY) {
        this.vScroller.saveButtonTop = this.vScroller.buttonTop
        this.vScroller.mouseY = e.clientY
        window.addEventListener('mousemove', this.vsbMouseMove)
        window.addEventListener('mouseup', this.vsbMouseUp)
        this.$refs.vScrollButton.classList.add('focus')
      }
    },
    vsbMouseUp () {
      window.removeEventListener('mousemove', this.vsbMouseMove)
      window.removeEventListener('mouseup', this.vsbMouseUp)
      this.lazy(() => {
        if (!this.$refs.vScrollButton) return
        this.$refs.vScrollButton.classList.remove('focus')
      })
      this.vScroller.mouseY = 0
      if (!this.noPaging) {
        const ratio = this.vScroller.buttonTop / (this.vScroller.height - this.vScroller.buttonHeight)
        this.pageTop = Math.round((this.table.length - this.pageSize) * ratio)
      }
      this.vScroller.runner = ''
      this.$forceUpdate()
    },
    vsbMouseMove (e) {
      if (e.buttons === 0)
        this.vsbMouseUp()
      else {
        const diff = e.clientY - this.vScroller.mouseY
        if (this.noPaging) {
          const ratio = (this.vScroller.saveButtonTop + diff) / (this.vScroller.height - this.vScroller.buttonHeight)
          this.tableContent.scrollTo(this.tableContent.scrollLeft, this.table.length * 24 * ratio)
        }
        else {
          this.vScroller.buttonTop = Math.max(0, Math.min(this.vScroller.height - this.vScroller.buttonHeight, this.vScroller.saveButtonTop + diff))
          this.$refs.vScrollButton.style.marginTop = this.vScroller.buttonTop + 'px'

          const ratio = this.vScroller.buttonTop / (this.vScroller.height - this.vScroller.buttonHeight)
          const recPos = Math.round((this.table.length - this.pageSize) * ratio) + 1
          const rec = this.table[recPos]
          this.vScroller.runner = recPos + '<br>' + this.fields
            .filter((field, i) => field.keyField || field.sticky || this.sortPos === i)
            .map(field => escapeHtml(field.label) + ': ' + escapeHtml(rec[field.name]))
            .join('<br>')
          this.$forceUpdate()
        }
      }
    },

    /* *** Horizontal Scrollbar *********************************************************************************
     */
    ftMouseDown (e) {
      const footerRect = this.footer.getBoundingClientRect()
      const ratio = (e.x - footerRect.left - 40) / (footerRect.width - 40)
      const fullWidth = this.systable.getBoundingClientRect().width
      const viewWidth = this.tableContent.getBoundingClientRect().width
      this.tableContent.scrollTo(fullWidth * ratio - viewWidth / 2, this.tableContent.scrollTop)
    },
    sbMouseDown (e) {
      e.stopPropagation()
      if (!this.$refs.hScroll) return
      if (!this.hScroller.mouseX) {
        const sleft = this.$refs.hScroll.getBoundingClientRect().left
        const fleft = this.footer.getBoundingClientRect().left + 40
        this.hScroller.left = sleft - fleft
        this.hScroller.mouseX = e.clientX
        window.addEventListener('mousemove', this.sbMouseMove)
        window.addEventListener('mouseup', this.sbMouseUp)
        this.$refs.hScroll.classList.add('focus')
      }
    },
    sbMouseUp () {
        window.removeEventListener('mousemove', this.sbMouseMove)
        window.removeEventListener('mouseup', this.sbMouseUp)
        this.lazy(() => {
          if (!this.$refs.hScroll) return
          this.$refs.hScroll.classList.remove('focus')
        })
        this.hScroller.mouseX = 0
        this.$forceUpdate()
    },
    sbMouseMove (e) {
      if (e.buttons === 0)
        this.sbMouseUp()
      else {
        const diff = e.clientX - this.hScroller.mouseX
        const ratio = (this.hScroller.left + diff) / this.hScroller.scrollerUnseenWidth
        this.tableContent.scrollTo(this.hScroller.tableUnseenWidth * ratio, this.tableContent.scrollTop)
      }
    },

    /* *** Window Event *******************************************************************************************
     */
    tableScroll () {
      if (this._tableScrollRaf != null) return
      this._tableScrollRaf = requestAnimationFrame(() => {
        this._tableScrollRaf = null
        this._tableScrollFlush()
      })
    },
    _tableScrollFlush () {
      this.showDatePicker = false
      this.autocompleteInputs = []
      if (this.focused && this.currentField)
        this.inputSquare.style.marginLeft =
          (this.currentField.sticky ? this.tableContent.scrollLeft - this.squareSavedLeft : 0) + 'px'

      if (this.tableContent.scrollTop !== this.vScroller.lastTop) {
        this.calVScroll()
        if (this.$refs.vScrollButton) {
          this.$refs.vScrollButton.classList.add('focus')
          this.lazy(() => this.$refs.vScrollButton.classList.remove('focus'), 1000)
        }
      }
      this.vScroller.lastTop = this.tableContent.scrollTop

      if (this.tableContent.scrollLeft !== this.hScroller.lastLeft) {
        if (this.$refs.hScroll && this.hScroller.tableUnseenWidth) {
          this.$refs.hScroll.classList.add('focus')
          this.lazy(() => this.$refs.hScroll.classList.remove('focus'), 1000)
          const ratio = this.tableContent.scrollLeft / this.hScroller.tableUnseenWidth
          this.$refs.hScroll.style.left = (this.hScroller.scrollerUnseenWidth * ratio) + 'px'
        }
      }
      this.hScroller.lastLeft = this.tableContent.scrollLeft
      if (this.cellRange || this.cellRangeDrag || this.fillDragActive || this.selectedCount > 0)
        this.updateCellRangeSelectionOverlay()
    },
    winScroll () {
      this.showDatePicker = false
      this.autocompleteInputs = []
    },
    mousewheel (e) {
      if (this.noMouseScroll || !this.mousein || !e.deltaY) return
      let adjust = 0
      if (e.deltaY > 1 * this.wheelSensitivity && this.pageTop + this.pageSize < this.table.length) adjust = 1
      else if (e.deltaY < -1 * this.wheelSensitivity && this.pageTop > 0) adjust = -1
      if (adjust) {
        this.pageTop += adjust
        setTimeout(this.calVScroll)
        if (this.$refs.vScrollButton) {
          this.$refs.vScrollButton.classList.add('focus')
          this.lazy(() => this.$refs.vScrollButton.classList.remove('focus'), 1000)
        }
      }
      e.preventDefault()
      e.stopPropagation()
      return false
    },
    winResize () {
      this.lazy(this.refreshPageSize, 500)
    },
    /** 剪贴板纯文本 → 二维数组（TSV / 多行单列，与 Excel 互操作） */
    parseClipboardToMatrix (text) {
      if (text == null || text === '') return null
      const normalized = String(text).replace(/\r\n/g, '\n').replace(/\r/g, '\n')
      const lines = normalized.split('\n')
      while (lines.length && lines[lines.length - 1] === '') lines.pop()
      if (lines.length === 0) return null
      return lines.map(line => line.split('\t'))
    },
    isMultiCellClipboardMatrix (matrix) {
      return !!(matrix && matrix.length && (matrix.length > 1 || (matrix[0] && matrix[0].length > 1)))
    },
    /** 以当前单元格为左上角写入矩阵；跳过只读 / key / 隐藏 / cellHtml */
    pasteClipboardMatrix (matrix) {
      if (!matrix || !matrix.length || this.currentRowPos < 0) return
      const anchorAbsR = this.pageTop + this.currentRowPos
      const anchorC = this.currentColPos
      for (let dr = 0; dr < matrix.length; dr++) {
        const rowCells = matrix[dr]
        if (!rowCells || !rowCells.length) continue
        const absR = anchorAbsR + dr
        if (absR < 0 || absR >= this.table.length) continue
        for (let dc = 0; dc < rowCells.length; dc++) {
          const col = anchorC + dc
          if (col < 0 || col >= this.fields.length) continue
          const field = this.fields[col]
          if (!field || field.readonly || field.keyField || field.invisible || field.cellHtml) continue
          const raw = rowCells[dc]
          try {
            this.updateCell(absR, field, field.toValue(raw == null ? '' : String(raw)))
          } catch (err) {}
        }
      }
    },
    /** 矩形选区 → TSV 字符串（供 Ctrl+C 写入剪贴板） */
    copyCellRangeAsTsvToClipboard () {
      const cr = this.cellRange
      if (!cr) return ''
      const r0 = Math.min(cr.r0, cr.r1)
      const r1 = Math.max(cr.r0, cr.r1)
      const c0 = Math.min(cr.c0, cr.c1)
      const c1 = Math.max(cr.c0, cr.c1)
      const lines = []
      for (let r = r0; r <= r1; r++) {
        const rec = this.table[r]
        const parts = []
        for (let c = c0; c <= c1; c++) {
          const f = this.fields[c]
          let cell = ''
          if (rec && f) {
            const t = f.toText(rec[f.name])
            cell = t == null ? '' : String(t)
            cell = cell.replace(/\r\n/g, '\n').replace(/\r/g, '\n').replace(/\n/g, ' ')
          }
          parts.push(cell)
        }
        lines.push(parts.join('\t'))
      }
      return lines.join('\n')
    },
    /** 行号列多选：选中行导出为 TSV（仅可见列） */
    copySelectedRowsAsTsv () {
      const keys = Object.keys(this.selected).map(Number).sort((a, b) => a - b)
      if (keys.length < 1) return ''
      const cols = this.fields.filter(f => !f.invisible)
      const lines = []
      for (const rowPos of keys) {
        const rec = this.table[rowPos]
        if (!rec) continue
        const parts = []
        for (const f of cols) {
          let cell = ''
          const t = f.toText(rec[f.name])
          cell = t == null ? '' : String(t)
          cell = cell.replace(/\r\n/g, '\n').replace(/\r/g, '\n').replace(/\n/g, ' ')
          parts.push(cell)
        }
        lines.push(parts.join('\t'))
      }
      return lines.join('\n')
    },
    winPaste (e) {
      if (e.target.tagName !== 'TEXTAREA') return
      if (!this.mousein && !this.focused) return
      if (!this.currentField || this.currentField.readonly) return
      if (this.inputBoxShow) {
        this.inputBoxChanged = true
        return
      }
      const cd = (e.originalEvent && e.originalEvent.clipboardData) || e.clipboardData
      const text = cd ? cd.getData('text/plain') : ''
      const matrix = this.parseClipboardToMatrix(text)
      if (this.isMultiCellClipboardMatrix(matrix)) {
        e.preventDefault()
        e.stopPropagation()
        this.pasteClipboardMatrix(matrix)
        return
      }
      this.inputCellWrite(text)
      e.preventDefault()
    },
    winKeyup (e) {
      if (!e.altKey) this.systable.classList.remove('alt')
      if (this.inputBoxShow && this.currentField.type === 'password') {
        setTimeout(() => {
          const v = this.inputBox.value.split('').map((c, i) => c === this.currentField.masking ? this.inputBox._value[i] : c)
          this.inputBox._value = v.join('')
          this.inputBox.value = this.currentField.masking.repeat(v.length)
        })
      }
    },
    winKeydown (e) {
      if (e.altKey) this.systable.classList.add('alt')
      if (e.keyCode === 27 && this.showDatePicker) {
        e.preventDefault()
        this.showDatePicker = false
        return
      }
      if (!this.mousein && !this.focused) return
      /* 行号多选后 Del/Backspace：删行由宿主处理（先于单元格清空逻辑） */
      if ((e.keyCode === 8 || e.keyCode === 46) && this.selectedCount > 0 &&
          !this.inputBoxShow && !this.cellRange && !this.fillDragActive) {
        e.preventDefault()
        this.$emit('delete-selected', this.getSelectedRecords())
        return
      }
      if (e.ctrlKey || e.metaKey)
        switch (e.keyCode) {
          case 90: // z
            this.undoTransaction()
            e.preventDefault()
            break
          case 65: // a
            this.toggleSelectAllRecords()
            e.preventDefault()
            break
          case 67: // c — 有矩形选区时复制整块为 TSV（便于 Ctrl+V 从当前格起粘贴）
            if (this.cellRange) {
              const tsv = this.copyCellRangeAsTsvToClipboard()
              if (tsv !== '') {
                this.inputBox.value = tsv
                this.inputBox.focus()
                this.inputBox.select()
                document.execCommand('copy')
                e.preventDefault()
                break
              }
            }
            /* 行号列多选 ≥2 行：复制选中行为 TSV */
            if (this.selectedCount >= 2) {
              const tsvRows = this.copySelectedRowsAsTsv()
              if (tsvRows !== '') {
                this.inputBox.value = tsvRows
                this.inputBox.focus()
                this.inputBox.select()
                document.execCommand('copy')
                e.preventDefault()
                break
              }
            }
            this.inputBox.value = this.currentCell.textContent
            this.inputBox.focus()
            this.inputBox.select()
            document.execCommand('copy')
            e.preventDefault()
            break
          case 70: // f
            if (!this.noFinding) {
              this.$refs.panelFind.showPanel()
              e.preventDefault()
            }
            break
          case 71: // g
            if (!this.noFindingNext && this.inputFind !== '') {
              this.doFindNext()
              e.preventDefault()
            }
            break
          case 76: // l
            e.preventDefault()
            this.calAutocompleteList(true)
            break
        }
      else {
        if (this.currentRowPos < 0) return
        switch (e.keyCode) {
          case 37:  // Left Arrow
            if (!this.focused) return
            if (!this.inputBoxShow) {
              this.moveWest(e)
              e.preventDefault()
            }
            else {
              if (this.inputBox.selectionStart === 0) {
                this.moveWest(e)
                e.preventDefault()
              }
            }
            break
          case 38:  // Up Arrow
            if (!this.focused) return
            e.preventDefault()
            if (this.autocompleteInputs.length === 0)
              this.moveNorth()
            else
            if (this.autocompleteSelect > 0) {
              this.autocompleteSelect--
              const showTop = this.autocompleteSelect * 23
              if (showTop < this.$refs.autocomplete.scrollTop)
                this.$refs.autocomplete.scrollTop = showTop
            }
            else
            if (this.autocompleteSelect === -1) {
              this.autocompleteSelect = 0
              // this.autocompleteSelect = this.autocompleteInputs.length - 1
            }
            break
          case 9:  // Tab
            if (!this.focused) return
            if (e.shiftKey) {
              if (!this.moveWest(e)) {
                if (this.moveNorth(e))
                  this.moveToEast(e)
                else
                  return this.inputBoxBlur()
              }
            }
            else {
              if (!this.moveEast(e)) {
                if (this.moveSouth(e))
                  this.moveToWest(e)
                else
                  return this.inputBoxBlur()
              }
            }
            e.preventDefault()
            break
          case 39: // Right Arrow
            if (!this.focused) return
            if (!this.inputBoxShow) {
              this.moveEast(e)
              e.preventDefault()
            }
            else {
              if (this.inputBox.selectionEnd === this.inputBox.value.length) {
                this.moveEast(e)
                e.preventDefault()
              }
            }
            break
          case 40:  // Down Arrow
            if (!this.focused) return
            e.preventDefault()
            if (this.autocompleteInputs.length === 0)
              this.moveSouth(e)
            else
              if (this.autocompleteSelect < this.autocompleteInputs.length - 1) {
                this.autocompleteSelect++
                if (this.autocompleteSelect >= 10) {
                  const showTop = this.autocompleteSelect * 23 - 206
                  const scrollTop = this.$refs.autocomplete.scrollTop
                  if (scrollTop < showTop)
                    this.$refs.autocomplete.scrollTop = showTop
                }
              }
            break
          case 13:  // Enter
            if (!this.focused) return
            e.preventDefault()
            if (this.autocompleteInputs.length === 0 || this.autocompleteSelect === -1) {
              if (this.enterToSouth)
                this.moveSouth(e)
              else
                this.moveEast(e)
            }
            else if (this.autocompleteSelect !== -1 && this.autocompleteSelect < this.autocompleteInputs.length) {
              this.inputAutocompleteText(this.autocompleteInputs[this.autocompleteSelect])
            }
            else {
              this.inputBox.value = this.currentCell.textContent
              this.inputBoxShow = 0
              this.inputBoxChanged = false
            }
            this.inputBoxComplete()
            break
          case 27:  // Esc
            if (!this.focused) return
            this.showDatePicker = false
            this.autocompleteInputs = []
            this.autocompleteSelect = -1
            if (this.inputBoxShow) {
              e.preventDefault()
              this.inputBox.value = this.currentCell.textContent
              this.inputBoxShow = 0
              this.inputBoxChanged = false
            }
            break
          case 33:  // Page Up
            this.prevPage()
            e.preventDefault()
            break
          case 34:  // Page Down
            this.nextPage()
            e.preventDefault()
            break
          case 8:   // Backspace
          case 46:  // Delete
            if (!this.focused) return
            if (this.inputBoxShow) {
              this.inputBoxChanged = true
              setTimeout(() => this.calAutocompleteList(true))
              return
            }
            if (this.autocompleteInputs.length) return
            /* 已提交矩形选区：批量清空可写格（上游无此能力，仅清当前格） */
            if (this.cellRange && !this.fillDragActive) {
              e.preventDefault()
              this.clearCellRangeContents()
              break
            }
            if (this.currentField.readonly) return
            e.preventDefault()
            this.inputBoxChanged = true
            this.inputBox.value = ''
            this.inputBoxComplete()
            break
          default:
            if (!this.focused) return
            if (this.currentField.readonly) return
            if (e.altKey) return
            if (e.key !== 'Process' && e.key.length > 1) return
            if (!this.inputBoxShow && this.currentField.type === 'date') {
              this.showDatePickerDiv()
              return
            }
            if (this.currentField.allowKeys) {
              if (this.currentField.allowKeys.constructor.name === 'Function') {
                if (!this.currentField.allowKeys(e.key.toUpperCase())) return e.preventDefault()
              }
              else
                if (this.currentField.allowKeys.indexOf(e.key.toUpperCase()) === -1) return e.preventDefault()
            }
            if (this.inputBoxShow && this.currentField.lengthLimit && this.inputBox.value.length >= this.currentField.lengthLimit) return e.preventDefault()
            if (!this.inputBoxShow) {
              if (['select', 'map', 'action'].includes(this.currentField.type)) {
                setTimeout(() => this.calAutocompleteList(true))
                if (e.keyCode === 32) return e.preventDefault()
                this.inputBox.value = ''
                this.inputBoxShow = 1
                this.inputBox.focus()
                return
              }
              this.inputBox.value = ''
              this.inputBoxShow = 1
              this.inputBox.focus()
              setTimeout(this.calAutocompleteList)
            }
            else {
              setTimeout(() => this.calAutocompleteList(this.autocompleteInputs.length))
            }
            this.inputBoxChanged = true
            break
        }
      }
    },

    /* *** Column Separator *******************************************************************************************
     */
    colSepMouseDown (e) {
      e.preventDefault()
      e.stopPropagation()
      const sepEl = this.domEventTargetEl(e)
      if (!sepEl) return
      if (this.allowAddCol && !sepEl.classList.contains('col-sep')) {
        sepEl.style.display = 'none'
        const me = sepEl.parentElement.parentElement
        const pos = Array.from(me.parentElement.children).findIndex(td => td === me)
        this.insertColumn(pos)
      }
      this.focused = false
      const getStyleVal = (elm, css) => {
        window.getComputedStyle(elm, null).getPropertyValue(css)
      }
      this.sep = {}
      // this.sep.curCol = this.colgroupTr.children[Array.from(this.labelTr.children).indexOf(e.target.parentElement)]
      this.sep.curCol = this.colgroupTr.children[
        Array.from(this.labelTr.children).indexOf(sepEl.parentElement) - (!this.noNumCol ? 0 : 1)]
      // this.sep.nxtCol = this.sep.curCol.nextElementSibling
      this.sep.pageX = e.pageX
      let padding = 0
      if (getStyleVal(this.sep.curCol, 'box-sizing') !== 'border-box') {
        const padLeft = getStyleVal(this.sep.curCol, 'padding-left')
        const padRight = getStyleVal(this.sep.curCol, 'padding-right')
        if (padLeft && padRight)
          padding = parseInt(padLeft) + parseInt(padRight)
      }
      this.sep.curColWidth = sepEl.parentElement.offsetWidth - padding
      // if (this.sep.nxtCol)
      //   this.sep.nxtColWidth = this.sep.nxtCol.offsetWidth - padding
      window.addEventListener('mousemove', this.colSepMouseMove)
      window.addEventListener('mouseup', this.colSepMouseUp)
    },
    colSepMouseOver (e) {
      const t = this.domEventTargetEl(e)
      if (!t) return
      if (t.classList.contains('col-sep')) {
        t.style.borderRight = '5px solid #cccccc'
        /* 与所在表头格同高；整表高度会导致竖条与列头不一致 */
        const th = t.closest && t.closest('th')
        const h = th && th.offsetHeight > 0 ? th.offsetHeight : this.systable.getBoundingClientRect().height
        t.style.height = h + 'px'
        if (this.allowAddCol)
          t.children[0].style.display = 'block'
      }
      else {
        // add-col-btn
        if (this.addColBtnTimeout) clearTimeout(this.addColBtnTimeout)
        if (this.allowAddCol)
          t.style.display = 'block'
      }
    },
    colSepMouseOut (e) {
      const t = this.domEventTargetEl(e)
      if (!t) return
      if (t.classList.contains('col-sep')) {
        t.style.borderRight = '5px solid transparent'
        t.style.height = '100%'
        this.addColBtnTimeout = setTimeout(() => {
          t.children[0].style.display = 'none'
        }, 500)
      }
      else {
        // add-col-btn
          t.style.display = 'none'
      }
    },
    colSepMouseMove (e) {
      if (!this.sep || !this.sep.curCol) return
      const diffX = e.pageX - this.sep.pageX
      this.sep.curCol.style.width = (this.sep.curColWidth + diffX) + 'px'
      this.lazy(this.calStickyLeft, 200)
    },
    colSepMouseUp (e) {
      e.preventDefault()
      e.stopPropagation()
      delete this.sep
      window.removeEventListener('mousemove', this.colSepMouseMove)
      window.removeEventListener('mouseup', this.colSepMouseUp)
      const setting = this.getSetting()
      if (this.remember) localStorage[window.location.pathname + '.' + this.token] = JSON.stringify(setting)
      this.$emit('setting', setting)
    },

    /* *** Finder *******************************************************************************************
     */
    doFindNext () {
      return this.doFind()
    },
    doFind (s) {
      if (typeof s === 'undefined') s = this.inputFind
      else this.inputFind = s
      s = s.toUpperCase()
      const row = Math.max(0, this.currentRowPos)
      for(let r = row + this.pageTop; r < this.table.length; r++) {
        const rec = this.table[r]
        for(let c = (r === row + this.pageTop ? this.currentColPos + 1: 0); c < this.fields.length; c++) {
          const field = this.fields[c].name
          if (typeof rec[field] !== 'undefined' && String(rec[field]).toUpperCase().indexOf(s) >= 0) {
            this.pageTop = this.findPageTop(r)
            setTimeout(() => {
              this.moveInputSquare(r - this.pageTop, c)
              setTimeout(() => this.inputBox.focus())
              this.focused = true
            })
            return true
          }
        }
      }
      for(let r = 0; r <= row + this.pageTop; r++) {
        const rec = this.table[r]
        for(let c = 0; c < (r === row + this.pageTop ? this.currentColPos : this.fields.length); c++) {
          const field = this.fields[c].name
          if (typeof rec[field] !== 'undefined' && String(rec[field]).toUpperCase().indexOf(s) >= 0) {
            this.pageTop = this.findPageTop(r)
            this.moveInputSquare(r - this.pageTop, c)
            setTimeout(() => {
              this.focused = true
            })
            return true
          }
        }
      }
      return false
    },
    findPageTop (rowPos) {
      for (let pt = this.pageTop; pt < this.table.length; pt += this.pageSize)
        if (rowPos >= pt && rowPos < pt + this.pageSize) return pt
      for (let pt = this.pageTop; pt > 0; pt -= this.pageSize)
        if (rowPos >= pt && rowPos < pt + this.pageSize) return pt
      return this.pageTop
    },

    /* *** Sort *******************************************************************************************
     */
    headerClick (e, colPos) {
      if(!this.noHeaderEdit && e.target.tagName === 'SPAN') {
        e.target.contentEditable = true
        e.target.addEventListener('focusout', this.completeHeaderChange)
        return
      }
      if (e.which === 1) {
        e.preventDefault()
        if (this.sortPos === colPos && this.sortDir > 0)
          this.sort(-1, colPos)
        else
          this.sort(1, colPos)
      }
    },
    completeHeaderChange (e) {
      const th = e.target.parentElement.parentElement
      const index = Array.from(th.parentElement.children).findIndex(v => v === th)
      this.fields[index - 1].label = e.target.textContent
    },
    sort (n, pos) {
      this.processing = true
      const colPos = typeof pos === 'undefined' ? this.columnFilterRef.colPos : pos
      const field = this.fields[colPos]
      const name = field.name
      setTimeout(() => {
        let sorting = field.sort
        if (sorting === null) {
          if (field.type === 'number')
            sorting = (a, b) => {
              if (Number(a[name]) > Number(b[name])) return 1
              if (Number(a[name]) < Number(b[name])) return -1
              return 0
            }
          else
              sorting = (a, b) => {
                return String(a[name]).localeCompare(String(b[name]))
              }
        }
        this.value.sort((a, b) => {
          return sorting(a, b) * -n
        })
        this.sortPos = colPos
        this.sortDir = n
        this.refresh()
        // this.$forceUpdate()
        this.processing = false
      }, 0)
    },

    /* *** Paging *******************************************************************************************
     */
    refreshPageSize () {
      if (this.$refs.hScroll) {
        const fullWidth = this.systable.getBoundingClientRect().width
        const viewWidth = this.tableContent.getBoundingClientRect().width
        this.hScroller.tableUnseenWidth = fullWidth - viewWidth
        this.$refs.hScroll.style.width = (100 * viewWidth / fullWidth) + '%'
        const scrollerWidth = this.$refs.hScroll.getBoundingClientRect().width
        this.hScroller.scrollerUnseenWidth = this.footer.getBoundingClientRect().width - 40 - scrollerWidth
      }
      if (!this.noPaging) {
        const offset = this.summaryRow ? 60 : 35
        let controlHeight = window.innerHeight - this.recordBody.getBoundingClientRect().top - offset
        const height = this.height.replace(/px/,'') * 1 + this.systable.getBoundingClientRect().top - this.recordBody.getBoundingClientRect().top
        if (this.height && controlHeight > height) controlHeight = height
        this.pageSize = this.page || Math.floor(controlHeight / 24)
        // this.pageSize = this.page || Math.floor((this.systable.parentNode.style.height - this.recordBody.getBoundingClientRect().top - offset) / 24)
      }
      else if (this.height === 'auto') {
        let h = Math.floor((window.innerHeight - this.tableContent.getBoundingClientRect().top - 25))
        let offset = 4
        if (this.filterRow) offset += 29
        if (this.summaryRow) offset += 25
        if (!this.footerRow) offset += 25
        h = Math.min(24 * (this.table.length - this.pageTop) + offset, h)
        this.systable.parentNode.style.height = h + 'px'
      }
      this.columnFillWidth()
      setTimeout(this.calVScroll)
    },
    firstPage (e) {
      if (e) e.stopPropagation()
      this.pageTop = 0
      this.calVScroll()
      if (this.$refs.vScrollButton) {
        setTimeout(() => {
          this.$refs.vScrollButton.classList.add('focus')
          this.lazy(() => {
            if (!this.$refs.vScrollButton) return
            this.$refs.vScrollButton.classList.remove('focus')
          }, 1000)
        })
      }
    },
    lastPage (e) {
      if (e) e.stopPropagation()
      this.pageTop = this.table.length - this.pageSize < 0 ? 0 : this.table.length - this.pageSize
      this.calVScroll()
      if (this.$refs.vScrollButton) {
        setTimeout(() => {
          this.$refs.vScrollButton.classList.add('focus')
          this.lazy(() => {
            if (!this.$refs.vScrollButton) return
            this.$refs.vScrollButton.classList.remove('focus')
          }, 1000)
        })
      }
    },
    prevPage (e) {
      if (e) e.stopPropagation()
      this.pageTop = this.pageTop < this.pageSize ? 0 : this.pageTop - this.pageSize
      this.calVScroll()
      if (this.$refs.vScrollButton) {
        setTimeout(() => {
          this.$refs.vScrollButton.classList.add('focus')
          this.lazy(() => {
            if (!this.$refs.vScrollButton) return
            this.$refs.vScrollButton.classList.remove('focus')
          }, 1000)
        })
      }
    },
    nextPage (e) {
      if (e) e.stopPropagation()
      if (this.pageTop + this.pageSize < this.table.length)
        this.pageTop = Math.min(this.pageTop + this.pageSize, this.table.length - this.pageSize)
      this.calVScroll()
      if (this.$refs.vScrollButton) {
        setTimeout(() => {
          this.$refs.vScrollButton.classList.add('focus')
          this.lazy(() => {
            if (!this.$refs.vScrollButton) return
            this.$refs.vScrollButton.classList.remove('focus')
          }, 1000)
        })
      }
    },

    /* *** Setting *******************************************************************************************
     */
    getSetting () {
      const colWidth = Array.from(this.colgroupTr.children).map(col => col.style.width)
      const fields = this.fields.map((field, i) => {
        return {
          name: field.name,
          invisible: field.invisible,
          width: colWidth[i + 1],
          label: field.label
        }
      })
      return {
        colHash: this.colHash,
        fields: fields
      }
    },

    settingClick () {},

    panelFilterClick(item) {
      if (!this.disablePanelFilter)
        this.$refs.panelFilter.showPanel(this.$refs[`filter-${item.name}`][0]);
    },

    /* *** Import/Export ************************************************************************************
     */
    importTable (cb, errCb) {
      this.$refs.importFile.click()
      this.importCallback = cb
      this.importErrorCallback = errCb
    },
    doImport (e) {
      this.processing = true
      // this.refresh()
      this.clearAllSelected()
      setTimeout(() => {
        const files = e.target.files
        if (!files || files.length === 0) return
        const file = files[0]

        const fileReader = new FileReader()
        fileReader.onload = async (e) => {
          try {
            const data = e.target.result
            const wb = XLSX.read(data, {type: 'binary', cellDates: true, cellStyle: false})
            const sheet = wb.SheetNames[0]
            let importData = XLSX.utils.sheet_to_row_object_array(wb.Sheets[sheet])
            importData = importData.filter(rec => Object.keys(rec).length > 0).map((rec) => {
              if (rec.key_1) {
                rec.key = rec.key_1  // Fixed the XLSX issue where key is set to be reserved word
                delete rec.key_1
              }
              Object.keys(rec).forEach(k => {
                if (typeof rec[k] === 'string') rec[k] = rec[k].replace(/[ \r\n\t]+$/g, '')
              })
              return rec
            })
            const keyStart = String(new Date().getTime() % 1e8)
            if (importData.length === 0) {
              if (this.importErrorCallback) this.importErrorCallback('noRecordIsRead')
              throw new Error('VueExcelEditor: ' + this.localizedLabel.noRecordIsRead)
            }
            if (this.fields
              .filter(f => f.keyField)
              .filter(f => typeof importData[0][f.name] === 'undefined' && typeof importData[0][f.label] === 'undefined').length > 0) {
                if (this.importErrorCallback) this.importErrorCallback('missingKeyColumn')
                throw new Error(`VueExcelEditor: ${this.localizedLabel.missingKeyColumn}`)
              }

            let pass = 0
            let inserted = 0
            let updated = 0
            while (pass < 2) {
              const keys = this.fields.filter(f => f.keyField)
              let uniqueKeys = []
              await Promise.all(importData.map(async (line, i) => {
                let rowPos = -1
                if (keys.length) {
                  // locate match record
                  rowPos = this.table.findIndex(v =>
                    keys.filter(f =>
                      typeof v[f.name] !== 'undefined'
                      && (v[f.name] === line[f.name] || v[f.name] === line[f.label])).length === keys.length
                  )
                  if (rowPos === -1) {
                    // If this is a new line, avoid the line with duplicate key
                    const linekey = keys.map(k => line[k.name] || line[k.label]).join(':')
                    if (linekey) {
                      if (uniqueKeys.includes(linekey)) return
                      uniqueKeys.push(linekey)
                    }
                  }
                }

                // if no match found, find an empty record
                if (rowPos === -1)
                  rowPos = this.table.findIndex(v => Object.keys(v).filter(f => !f.startsWith('$')).length === 0)

                const rec = {
                  $id: typeof line.$id === 'undefined' ? keyStart + '-' + ('000000' + i).slice(-7) : line.$id
                }

                // Raise exception if readonly not not pass validation
                await Promise.all(this.fields.map(async (field) => {
                  if (field.name.startsWith('$')) return
                  let val = line[field.name]
                  if (typeof val === 'undefined') val = line[field.label]
                  if (typeof val === 'undefined') val = null
                  else {
                    if (field.readonly) {
                      if (this.importErrorCallback) this.importErrorCallback('readonlyColumnDetected', i+1)
                      throw new Error(`VueExcelEditor: [row=${i+1}] ` + this.localizedLabel.readonlyColumnDetected + ': ' + field.name)
                    }
                    if (field.change) {
                      let result = await field.change(val, rec[field.name], rec, field)
                      if (result === false) {
                        if (this.importErrorCallback) this.importErrorCallback('columnHasValidationError', i+1)
                        throw new Error(`VueExcelEditor: [row=${i+1}, val=${val}] ` + this.localizedLabel.columnHasValidationError(field.name, ''))
                      }
                    }
                    if (field.validate) {
                      let err
                      if ((err = field.validate(val, rec[field.name], rec, field))) {
                        if (this.importErrorCallback) this.importErrorCallback('columnHasValidationError', i+1, val)
                        throw new Error(`VueExcelEditor: [row=${i+1}, val=${val}] ` + this.localizedLabel.columnHasValidationError(field.name, err))
                      }
                    }
                    if (this.validate) {
                      let err
                      if ((err = this.validate(val, rec[field.name], rec, field))) {
                        if (this.importErrorCallback) this.importErrorCallback('rowHasValidationError', i+1, val)
                        throw new Error(`VueExcelEditor: [row=${i+1}, val=${val}] ` + this.localizedLabel.rowHasValidationError(i + 1, field.name, err))
                      }
                    }
                  }
                  if (val !== null) rec[field.name] = val
                  else if (field.mandatory) {
                    if (this.importErrorCallback) this.importErrorCallback(field.mandatory, i+1, val)
                    throw new Error(`VueExcelEdutor: [row=${i+1}, val=${val}] ` + field.mandatory)
                  }
                }))

                // Do actual insert/update if 2nd pass
                if (pass === 1) {
                  if (rowPos >= 0) {
                    updated++
                    Object.keys(rec).forEach(name => {
                      if (name.startsWith('$')) return
                      this.updateCell(rowPos, name, rec[name])
                    })
                    this.selected[rowPos] = this.table[rowPos].$id
                  }
                  else {
                    this.newRecord(rec, true)
                    inserted++
                  }
                }
              }))
              pass++
            }
            if (pass === 2 && this.importCallback) {
              this.importCallback({
                inserted: inserted,
                updated: updated,
                recordAffected: inserted + updated
              })
            }
          }
          catch (e) {
            if (this.importErrorCallback) this.importErrorCallback(e.message)
            throw new Error('VueExcelEditor: ' + e.stack)
          }
          finally {
            this.processing = false
            this.$refs.importFile.value = ''
          }
        }
        fileReader.onerror = (e) => {
          this.processing = false
          this.$refs.importFile.value = ''
          if (this.importErrorCallback) this.importErrorCallback(e.message)
          throw new Error('VueExcelEditor: ' + e.stack)
        }
        fileReader.readAsBinaryString(file)
      }, 500)
    },
    exportTable (format, selectedOnly, filename) {
      this.processing = true
      setTimeout(() => {
        const wb = XLSX.utils.book_new()
        let ws1 = null
        let data = this.table
        if (selectedOnly)
          data = this.table.filter((rec, i) => this.selected[i])
        const mapped = data.map(rec => {
          const conv = {}
          this.fields.forEach(field => conv[field.name] = rec[field.name])
          return conv
        })
        ws1 = XLSX.utils.json_to_sheet(mapped, {
          header: this.fields.map(field => field.name)
        })
        const labels = Array.from(this.labelTr.children).slice(1).map(t => t.children[0].innerText)
        XLSX.utils.sheet_add_aoa(ws1, [labels], {origin: 0})
        ws1['!cols'] = Array.from(this.labelTr.children).slice(1).map((t) => {
          return {
            width: t.getBoundingClientRect().width / 6.5
          }
        })
        XLSX.utils.book_append_sheet(wb, ws1, 'Sheet1')
        filename = filename || 'export'
        switch (format) {
          case 'csv':
            if (!filename.endsWith('.csv')) filename = filename + '.csv'
            break
          case 'xls':
            if (!filename.endsWith('.xls')) filename = filename + '.xls'
            break
          case 'xlsx':
          case 'excel':
          default:
            if (!filename.endsWith('.xlsx')) filename = filename + '.xlsx'
            break
        }
        if (filename.endsWith('.xlsx'))
          XLSX.writeFile(wb, filename, {
            compression: 'DEFLATE',
            compressionOptions: {
              level: 6
            }
          })
        else
          XLSX.writeFile(wb, filename)

        this.processing = false
      }, 500)
    },

    /* *** Select *******************************************************************************************
     */
    getSelectedRecords () {
      return this.table.filter((rec, i) => this.selected[i])
    },
    /**
     * 行号列多行选择与「单格焦点 / 矩形区域选」互斥：进入行选前卸掉单元格编辑态与区域选外框。
     */
    exclusiveClearSheetFocusForRowSelect () {
      this.fillHandleCleanup()
      this.cellRangeSelectCleanupListeners()
      this.cellRange = null
      this.$nextTick(() => this.updateCellRangeSelectionOverlay())
      if (!this.focused) return
      if (this.inputBoxChanged && this.currentRowPos >= 0 && this.currentColPos >= 0 && this.inputBox) {
        const value = this.inputBox._value || this.inputBox.value
        this.inputBox._value = ''
        this.inputCellWrite(value, this.currentColPos, this.pageTop + this.currentRowPos)
        this.inputBoxChanged = false
      }
      this.inputBoxShow = 0
      this.showDatePicker = false
      this.autocompleteInputs = []
      if (this.currentRowPos >= 0 && this.recordBody && this.currentRowPos < this.recordBody.children.length) {
        const row = this.recordBody.children[this.currentRowPos]
        if (row && row.children && row.children[0]) row.children[0].classList.remove('focus')
      }
      if (this.currentColPos >= 0 && this.labelTr && this.labelTr.children && this.labelTr.children[this.currentColPos + 1])
        this.labelTr.children[this.currentColPos + 1].classList.remove('focus')
      if (this.$refs.inputBox) this.$refs.inputBox.blur()
      this.focused = false
    },
    /*
    deleteSelectedRecords () {
      this.table = this.table.filter((rec, i) => typeof this.selected[i] === 'undefined')
      this.selected = {}
      this.selectedCount = 0
    },
    */
    rowLabelClick (e) {
      let target = this.domEventTargetEl(e)
      while (target && target.tagName !== 'TD') target = target.parentNode
      if (!target) return
      this.exclusiveClearSheetFocusForRowSelect()
      const rowPos = Number(target.getAttribute('pos')) + this.pageTop
      this.rowLabelClickOnRow(e, rowPos)
    },
    rowLabelDragCleanup () {
      if (this._rowLabelDragMoveBound) {
        document.removeEventListener('mousemove', this._rowLabelDragMoveBound)
        this._rowLabelDragMoveBound = null
      }
      if (this._rowLabelDragUpBound) {
        document.removeEventListener('mouseup', this._rowLabelDragUpBound, true)
        this._rowLabelDragUpBound = null
      }
      if (this._rowLabelDragRaf) {
        cancelAnimationFrame(this._rowLabelDragRaf)
        this._rowLabelDragRaf = null
      }
      this.rowLabelDrag = null
    },
    rowLabelClickOnRow (e, rowPos) {
      if (e.shiftKey) {
        document.getSelection().removeAllRanges()
        if (this.prevSelect !== -1 && this.prevSelect !== rowPos) {
          e.preventDefault()
          if (rowPos > this.prevSelect)
            for (let i = this.prevSelect; i <= rowPos; i++)
              this.selectRecord(i)
          else
            for (let i = rowPos; i <= this.prevSelect; i++)
              this.selectRecord(i)
        }
      }
      else {
        /* 无修饰键：点行号始终选中该行（独占），再点同一行保持选中，不做 toggle 取消 */
        if (!this.freeSelect && !(e.ctrlKey || e.metaKey)) {
          this.clearAllSelected()
          this.selectRecord(rowPos)
        } else {
          const wasSelected = typeof this.selected[rowPos] !== 'undefined'
          if (!wasSelected) this.selectRecord(rowPos)
          else this.unSelectRecord(rowPos)
        }
      }
      this.prevSelect = rowPos
    },
    rowLabelMouseDown (e) {
      if (e.button !== 0) return
      const td = this.domEventTargetTd(e)
      if (!td || !td.classList.contains('first-col')) return
      this.exclusiveClearSheetFocusForRowSelect()
      const tr = td.parentNode
      if (!tr || tr.parentNode !== this.recordBody) return
      const idx = Array.from(this.recordBody.children).indexOf(tr)
      if (idx < 0) return
      const anchorAbs = this.pageTop + idx
      this.rowLabelDrag = {
        anchorAbs,
        lastAbs: anchorAbs,
        additive: !!(e.ctrlKey || e.metaKey),
        moved: false
      }
      this._rowLabelDragMoveBound = this.rowLabelDragMove.bind(this)
      this._rowLabelDragUpBound = this.rowLabelDragUp.bind(this)
      document.addEventListener('mousemove', this._rowLabelDragMoveBound)
      document.addEventListener('mouseup', this._rowLabelDragUpBound, true)
      e.preventDefault()
    },
    rowLabelDragMove (e) {
      if (!this.rowLabelDrag) return
      const el = document.elementFromPoint(e.clientX, e.clientY)
      let node = el
      while (node && node.tagName !== 'TR') node = node.parentElement
      if (!node || node.parentNode !== this.recordBody) return
      const idx = Array.from(this.recordBody.children).indexOf(node)
      if (idx < 0) return
      const abs = this.pageTop + idx
      if (abs !== this.rowLabelDrag.lastAbs) this.rowLabelDrag.moved = true
      this.rowLabelDrag.lastAbs = abs
      this.scheduleRowLabelDragSelect(this.rowLabelDrag.anchorAbs, abs, this.rowLabelDrag.additive)
    },
    scheduleRowLabelDragSelect (a, b, additive) {
      if (this._rowLabelDragRaf) cancelAnimationFrame(this._rowLabelDragRaf)
      this._rowLabelDragRaf = requestAnimationFrame(() => {
        this._rowLabelDragRaf = null
        this.applyRowLabelDragSelect(a, b, additive)
      })
    },
    applyRowLabelDragSelect (a, b, additive) {
      const lo = Math.min(a, b)
      const hi = Math.max(a, b)
      if (!additive) {
        if (this.selectedCount > 0) {
          const prevKeys = Object.keys(this.selected).map(Number)
          this.$emit('select', prevKeys, false)
          if (this.recordBody) {
            prevKeys.forEach((rowPos) => {
              const tr = this.recordBody.children[rowPos - this.pageTop]
              if (tr) tr.classList.remove('select')
            })
          }
        }
        this.selected = {}
        this.selectedCount = 0
      }
      for (let i = lo; i <= hi; i++) {
        if (i < 0 || i >= this.table.length) continue
        if (additive && typeof this.selected[i] !== 'undefined') continue
        this.$set(this.selected, i, this.table[i].$id)
        const tr = this.recordBody && this.recordBody.children[i - this.pageTop]
        if (tr) tr.classList.add('select')
      }
      this.selectedCount = Object.keys(this.selected).length
    },
    rowLabelDragUp (e) {
      if (this._rowLabelDragMoveBound) {
        document.removeEventListener('mousemove', this._rowLabelDragMoveBound)
        this._rowLabelDragMoveBound = null
      }
      if (this._rowLabelDragUpBound) {
        document.removeEventListener('mouseup', this._rowLabelDragUpBound, true)
        this._rowLabelDragUpBound = null
      }
      if (this._rowLabelDragRaf) {
        cancelAnimationFrame(this._rowLabelDragRaf)
        this._rowLabelDragRaf = null
      }
      const d = this.rowLabelDrag
      this.rowLabelDrag = null
      if (!d) return
      if (d.moved || d.anchorAbs !== d.lastAbs) {
        this.applyRowLabelDragSelect(d.anchorAbs, d.lastAbs, d.additive)
        this.prevSelect = d.lastAbs
      } else {
        const ev = {
          shiftKey: !!e.shiftKey,
          ctrlKey: !!e.ctrlKey,
          metaKey: !!e.metaKey,
          preventDefault () {},
          stopPropagation () {}
        }
        this.rowLabelClickOnRow(ev, d.anchorAbs)
      }
    },
    selectAllClick () {},
    reviseSelectedAfterTableChange () {
      this.rowIndex = {}
      this.table.forEach((rec, i) => (this.rowIndex[rec.$id] = i))
      const temp = Object.assign(this.selected)
      this.selected = {}
      Object.keys(temp).forEach((p) => {
        const id = temp[p]
        if (typeof this.rowIndex[id] !== 'undefined')
          this.selected[this.rowIndex[id]] = id
      })
      this.selectedCount = Object.keys(this.selected).length
    },
    toggleSelectRecord (rowPos) {
      if (typeof this.selected[rowPos] !== 'undefined') this.unSelectRecord(rowPos)
      else this.selectRecord(rowPos)
    },
    selectRecord (rowPos) {
      if (typeof this.selected[rowPos] === 'undefined') {
        this.selectedCount++
        this.$set(this.selected, rowPos, this.table[rowPos].$id)
        if (this.recordBody.children[rowPos - this.pageTop])
          this.recordBody.children[rowPos - this.pageTop].classList.add('select')
        this.lazy(rowPos, (buf) => {
          this.$emit('select', buf, true)
        })
      }
    },
    selectRecordByKeys (keys) {
      const rowPos = this.table.findIndex(v =>
        this.fields.filter(f => f.keyField).filter(f => v[f.name] === keys[f.name]).length === keys.length)
      if (rowPos >= 0) this.selectRecord(rowPos)
    },
    selectRecordById (id) {
      const rowPos = this.table.findIndex(v => v.$id === id)
      if (rowPos >= 0) this.selectRecord(rowPos)
    },
    unSelectRecord (rowPos) {
      if (typeof this.selected[rowPos] !== 'undefined') {
        delete this.selected[rowPos]
        this.selectedCount--
        if (this.recordBody.children[rowPos - this.pageTop])
          this.recordBody.children[rowPos - this.pageTop].classList.remove('select')
        this.lazy(rowPos, (buf) => {
          this.$emit('select', buf, false)
        })
      }
    },
    toggleSelectAllRecords (e) {
      if (e) e.preventDefault()
      if (this.selectedCount > 0)
        this.clearAllSelected()
      else {
        for (let i = 0; i < this.table.length; i++)
          this.selectRecord(i)
        this.selectedCount = this.table.length
      }
    },
    clearAllSelected () {
      if (this.selectedCount > 0) {
        const keys = Object.keys(this.selected).map(Number)
        this.$emit('select', keys, false)
        if (this.recordBody) {
          keys.forEach((rowPos) => {
            const tr = this.recordBody.children[rowPos - this.pageTop]
            if (tr) tr.classList.remove('select')
          })
        }
      }
      this.selected = {}
      this.selectedCount = 0
    },

    /* *** Cursor *******************************************************************************************
     */
    moveTo (rowPos, colPos) {
      colPos = colPos || 0
      const done = this.moveInputSquare(rowPos - this.pageTop, colPos)
      this.focused = true
      setTimeout(() => this.inputBox.focus())
      return done
    },
    moveToNorthWest() {
      let goColPos = 0
      while (this.fields[goColPos].invisible && goColPos < this.fields.length - 1) goColPos++
      return this.moveTo(0, goColPos)
    },
    moveToNorthEast () {
      let goColPos = this.fields.length - 1
      while (this.fields[goColPos].invisible && goColPos > 0) goColPos--
      return this.moveTo(0, goColPos)
    },
    moveToSouthWest () {
      let goRowPos = this.table.length - 1
      let goColPos = 0
      while (this.fields[goColPos].invisible && goColPos < this.fields.length - 1) goColPos++
      return this.moveTo(goRowPos, goColPos)
    },
    moveToSouthEast () {
      let goRowPos = this.table.length - 1
      let goColPos = this.fields.length - 1
      while (this.fields[goColPos].invisible && goColPos > 0) goColPos--
      return this.moveTo(goRowPos, goColPos)
    },
    moveToWest () {
      let goRowPos = this.currentRowPos
      let goColPos = 0
      while (this.fields[goColPos].invisible && goColPos < this.fields.length - 1) goColPos++
      return this.moveTo(goRowPos, goColPos)
    },
    moveToEast () {
      let goRowPos = this.currentRowPos
      let goColPos = this.fields.length - 1
      while (this.fields[goColPos].invisible && goColPos > 0) goColPos--
      return this.moveTo(goRowPos, goColPos)
    },
    moveWest () {
      if (this.focused && this.currentColPos > 0) {
        let goColPos = this.currentColPos - 1
        while (this.fields[goColPos].invisible && goColPos > 0) goColPos--
        if (goColPos === -1 || this.fields[goColPos].invisible) return false
        return this.moveInputSquare(this.currentRowPos, goColPos)
      }
      return false
    },
    moveEast () {
      if (this.focused && this.currentColPos < this.fields.length - 1) {
        let goColPos = this.currentColPos + 1
        while (this.fields[goColPos].invisible && goColPos < this.fields.length - 1) goColPos++
        if (goColPos === this.fields.length || this.fields[goColPos].invisible) return false
        return this.moveInputSquare(this.currentRowPos, goColPos)
      }
      return false
    },
    moveNorth () {
      if (this.focused) {
        const done = this.moveInputSquare(this.currentRowPos - 1, this.currentColPos)
        this.calVScroll()
        if (this.$refs.vScrollButton) {
          setTimeout(() => {
            this.$refs.vScrollButton.classList.add('focus')
            this.lazy(() => {
              if (!this.$refs.vScrollButton) return
              this.$refs.vScrollButton.classList.remove('focus')
            }, 1000)
          })
        }
        return done
      }
      return false
    },
    moveSouth () {
      if (this.focused) {
        // if (this.currentRowPos + 1 >= this.table.length) {
        if (this.currentRowPos+1 >= (this.pageBottom - this.pageTop) && this.pageBottom >= this.table.length) {
          if (this.readonly) return false
          if (!this.newIfBottom) return false
          this.newRecord({}, false, true)
          setTimeout(this.moveSouth, 0)
          return true
        }
        const done = this.moveInputSquare(this.currentRowPos + 1, this.currentColPos)
        this.calVScroll()
        if (this.$refs.vScrollButton) {
          setTimeout(() => {
            this.$refs.vScrollButton.classList.add('focus')
            this.lazy(() => {
              if (!this.$refs.vScrollButton) return
              this.$refs.vScrollButton.classList.remove('focus')
            }, 1000)
          })
        }
        return done
      }
      return false
    },
    mouseDown (e) {
      const el = this.domEventTargetTd(e)
      if (!el) return
      if (el.parentNode.parentNode.tagName === 'TBODY' && !el.classList.contains('first-col')) {
        if (this.selectedCount > 0) this.clearAllSelected()
        const row = el.parentNode
        const colPos = Array.from(row.children).indexOf(el) - 1
        const rowPos = Array.from(row.parentNode.children).indexOf(row)
        this.currentField = this.fields[colPos]
        if (!this.currentField) return
        e.preventDefault()
        setTimeout(() => this.inputBox.focus())
        this.focused = true
        const relX = e.clientX - el.getBoundingClientRect().left
        this.$emit('cell-click', {rowPos, colPos})
        if (typeof this.currentField.cellClick === 'function')
          this.currentField.cellClick(this.currentCell.textContent, this.currentRecord, rowPos, colPos, this.currentField, this)
        this.moveInputSquare(rowPos, colPos)
        this.cellRangeSelectArm(rowPos, colPos)
        if (this.currentField && this.currentField.link /* && e.altKey */)
          setTimeout(() => this.currentField.link(this.currentCell.textContent, this.currentRecord, rowPos, colPos, this.currentField, this))
        if (this.currentField.listByClick) {
          if (!this.tdDropdownHotZoneHit(el, e.clientX)) return
          this.cellRangeSelectCleanupListeners()
          return this.calAutocompleteList(true)
        }
        if (el.offsetWidth < el.scrollWidth) {
          // show textTip
          this.textTip = this.currentCell.textContent
          this.$refs.texttip.style.opacity = 0
          const rect = el.getBoundingClientRect()
          setTimeout(() => {
            const r = this.$refs.texttip.getBoundingClientRect()
            if (rect.bottom + r.height > window.innerHeight) {
              // show at top
              this.$refs.texttip.style.top = (rect.top - r.height) + 'px'
            }
            else {
              this.$refs.texttip.style.top = rect.bottom + 'px'
            }
            if (rect.left + r.width > window.innerWidth)
              this.$refs.texttip.style.left = (rect.right - r.width) + 'px'
            else
              this.$refs.texttip.style.left = rect.left + 'px'
            this.$refs.texttip.style.opacity = 1
          })
        }
        if (this.currentField.readonly) return
        this.inputBox.value = this.currentCell.textContent
        /* 仅点击右侧下拉/日期三角热区时弹出面板，点击单元格正文不打开 */
        const tri = this.tdDropdownHotZoneHit(el, e.clientX)
        /* 与 listByClick 分支一致：三角打开下拉/日期前须卸掉 cellRangeSelectArm 注册的 document mouseup，
         * 否则同一次点击的 mouseup → cellRangeSelectUp → moveInputSquare 会清空 autocompleteInputs / 关掉日期面板 */
        if (el.classList.contains('select') && tri) {
          this.cellRangeSelectCleanupListeners()
          this.calAutocompleteList(true)
        } else if (el.classList.contains('datepick') && tri) {
          this.cellRangeSelectCleanupListeners()
          this.showDatePickerDiv()
        }
      }
    },
    /** 覆盖层 pointer-events:none 后，单击不再触发 inputSquareClick；双击进入内联编辑（与原先单击绿框一致） */
    cellDoubleClick (e) {
      const el = this.domEventTargetTd(e)
      if (!el) return
      if (el.parentNode.parentNode.tagName !== 'TBODY' || el.classList.contains('first-col')) return
      if (this.selectedCount > 0) this.clearAllSelected()
      const row = el.parentNode
      const colPos = Array.from(row.children).indexOf(el) - 1
      const rowPos = Array.from(row.parentNode.children).indexOf(row)
      const field = this.fields[colPos]
      if (!field || field.readonly || field.type === 'select') return
      e.preventDefault()
      this.moveInputSquare(rowPos, colPos)
      if (!this.inputBoxShow) {
        this.inputBox.value = this.currentCell.textContent
        this.inputBoxShow = 1
        this.inputBox.focus()
        this.inputBoxChanged = false
        this.focused = true
      }
    },
    cellMouseMove (e) {
      const td = this.domEventTargetTd(e)
      if (!td) return
      let cursor = 'cell'
      if (this.inputBoxShow) cursor = 'default'
      if (this.tdDropdownHotZoneHit(td, e.clientX)) {
        if (!td.classList.contains('readonly') && (td.classList.contains('select') || td.classList.contains('datepick')))
          cursor = 'pointer'
      }
      if (td.offsetWidth < td.scrollWidth)
        cursor = 'pointer'
      const row = td.parentNode
      if (!row) return
      const colPos = Array.from(row.children).indexOf(td) - 1
      const currentField = this.fields[colPos]
      if (currentField && currentField.type === 'action')
        cursor = 'pointer'
      td.style.cursor = cursor
    },
    cellMouseOver (e) {
      const cell = this.domEventTargetTd(e)
      if (!cell || !cell.classList || !cell.classList.contains('error')) return
      if (this.tipTimeout) clearTimeout(this.tipTimeout)
      if ((this.tip = this.errmsg[cell.getAttribute('id')]) === '') return
      const rect = cell.getBoundingClientRect()
      this.$refs.tooltip.style.top = (rect.top - 14) + 'px'
      this.$refs.tooltip.style.left = (rect.right + 8) + 'px'
      cell.addEventListener('mouseout', this.cellMouseOut)
    },
    numcolMouseOver (e) {
      const cell = this.domEventTargetTd(e)
      if (!cell || !cell.classList || !cell.classList.contains('error')) return
      if (this.tipTimeout) clearTimeout(this.tipTimeout)
      if ((this.tip = this.rowerr[cell.getAttribute('id')]) === '') return
      const rect = cell.getBoundingClientRect()
      this.$refs.tooltip.style.top = (rect.top - 14) + 'px'
      this.$refs.tooltip.style.left = (rect.right + 8) + 'px'
      cell.addEventListener('mouseout', this.cellMouseOut)
    },
    cellMouseOut (e) {
      this.tipTimeout = setTimeout(() => {
        this.tip = ''
      }, 1000)
      const el = this.domEventTargetTd(e)
      if (el) el.removeEventListener(e.type, this.cellMouseOut)
    },
    mouseOver () {
      this.mousein = true
      this.systable.classList.add('mouseover')
    },
    mouseOut () {
      this.mousein = false
      this.systable.classList.remove('mouseover')
    },

    /* *** InputBox *****************************************************************************************
     */
    /** 可编辑纯文本列：按内容收缩 textarea 高度；有 options 的列跳过 */
    syncDefectNameInputBoxLayout () {
      const ta = this.inputBox
      const f = this.currentField
      if (!ta || !this.inputBoxShow || !f || f.readonly || f.type === 'date' || f.options) return
      const wrap = ta.parentElement
      if (!wrap) return
      ta.style.setProperty('height', 'auto', 'important')
      ta.style.setProperty('min-height', '0', 'important')
      const maxH = Math.max(0, wrap.clientHeight)
      let h = ta.scrollHeight
      if (maxH > 0) h = Math.min(h, maxH)
      ta.style.setProperty('height', h + 'px', 'important')
    },
    onInputBoxNativeInput () {
      if (this.inputBoxShow) this.inputBoxChanged = true
      const f = this.currentField
      if (f && !f.readonly && f.type !== 'date' && !f.options && this.inputBoxShow)
        this.$nextTick(() => this.syncDefectNameInputBoxLayout())
    },
    moveInputSquare (rowPos, colPos) {
      const prevR = this.currentRowPos
      const prevC = this.currentColPos
      this.textTip = ''
      if (colPos < 0) return false
      const top = this.pageTop
      let row = this.recordBody.children[rowPos]
      if (!row) {
        if (rowPos > this.currentRowPos) {
          // move the whole page down 1 record
          if (this.pageTop + this.pageSize < this.table.length)
            this.pageTop += 1
          else
            return false
          row = this.recordBody.children[--rowPos]
        }
        else {
          // move the whole page up 1 record
          if (this.pageTop - 1 >= 0)
            this.pageTop -= 1
          else
            return false
          row = this.recordBody.children[++rowPos]
        }
      }

      // Clear the label markers
      this.labelTr.children[this.currentColPos + 1].classList.remove('focus')
      if (this.currentRowPos >= 0 && this.currentRowPos < this.pagingTable.length)
        this.recordBody.children[this.currentRowPos].children[0].classList.remove('focus')

      // Off the textarea when moving, write to value if changed
      if (this.inputBoxShow) this.inputBoxShow = 0
      if (this.inputBoxChanged) {
        const value = this.inputBox._value || this.inputBox.value
        this.inputBox._value = ''
        this.inputCellWrite(value, this.currentColPos, top + this.currentRowPos)
        this.inputBoxChanged = false
      }

      // Relocate the inputSquare
      const cell = row.children[colPos + 1]
      if (!cell) return false
      this.currentField = this.fields[colPos]
      const cellRect = cell.getBoundingClientRect()
      const tableRect = this.systable.getBoundingClientRect()
      this.squareSavedLeft = this.tableContent.scrollLeft
      this.inputSquare.style.marginLeft = 0
      this.inputSquare.style.left = (cellRect.left - tableRect.left - 1) + 'px'
      this.inputSquare.style.top =  (cellRect.top - tableRect.top - 1) + 'px'
      this.inputSquare.style.width = (cellRect.width + 1) + 'px'
      this.inputSquare.style.height = (cellRect.height + 1) + 'px'
      this.inputSquare.style.zIndex = this.currentField.sticky ? 3 : 1

      // Adjust the scrolling to display the whole focusing cell
      if (!this.currentField.sticky) {
        const boundRect = this.$el.getBoundingClientRect()
        if (cellRect.right >= boundRect.right - 12)
          this.tableContent.scrollBy(cellRect.right - boundRect.right + 13, 0)
        if (cellRect.left <= boundRect.left + this.leftMost)
          this.tableContent.scrollBy(cellRect.left - boundRect.left - this.leftMost - 1, 0)
      }

      this.currentRowPos = rowPos
      this.currentColPos = colPos
      this.currentCell = cell
      this.currentRecord = this.table[top + rowPos]

      this.$emit('cell-focus', {rowPos, colPos, cell, record: this.currentRecord})

      // Off all editors
      if (this.showDatePicker) this.showDatePicker = false
      if (this.autocompleteInputs.length) {
        this.autocompleteInputs = []
        this.autocompleteSelect = -1
      }
      if (this.recalAutoCompleteList) clearTimeout(this.recalAutoCompleteList)

      // set the label markers
      if (this.currentRowPos >= 0 && this.currentRowPos < this.pagingTable.length) {
        this.inputBox.value = this.currentCell.textContent
        this.inputBox.focus()
        this.focused = true
        row.children[0].classList.add('focus')
        this.labelTr.children[colPos + 1].classList.add('focus')
      }
      if (!this.suppressClearCellRangeOnMove && (prevR !== this.currentRowPos || prevC !== this.currentColPos)) {
        this.cellRange = null
      }
      this.$nextTick(() => this.updateCellRangeSelectionOverlay())
      return true
    },
    inputSquareClick () {
      if (!this.currentField.readonly && !this.inputBoxShow && this.currentField.type !== 'select') {
        this.inputBox.value = this.currentCell.textContent
        this.inputBoxShow = 1
        this.inputBox.focus()
        this.inputBoxChanged = false
        this.focused = true
      }
    },
    inputBoxMouseMove (e) {
      let cursor = 'text'
      if (!this.currentField.readonly
        && (this.currentField.options || this.currentField.type === 'date')
        && e.target.offsetWidth - e.offsetX < 24)
        cursor = 'pointer'
      e.target.style.cursor = cursor
    },
    inputBoxMouseDown (e) {
      if (e.target.offsetWidth - e.offsetX > 24) return
      if (this.currentField.readonly) return
      if (this.currentField.options) {
        e.preventDefault()
        this.calAutocompleteList(true)
      }
      if (this.currentField.type === 'date') {
        e.preventDefault()
        this.showDatePickerDiv()
      }
    },
    inputCellWrite (setText, colPos, recPos) {
      let field = this.currentField
      if (typeof colPos !== 'undefined') field = this.fields[colPos]
      if (typeof recPos === 'undefined') recPos = this.pageTop + this.currentRowPos
      if (typeof this.selected[recPos] !== 'undefined')
        this.updateSelectedRows(field, field.toValue(setText))
      else
        this.updateCell(recPos, field, field.toValue(setText))
    },
    inputBoxBlur () {
      if (!this.$refs.dpContainer) return
      if (this.$refs.dpContainer.querySelector(':hover')) return
      this.inputBoxComplete()
      this.focused = false
      if (this.currentRowPos !== -1 && this.currentRowPos < this.recordBody.children.length) {
        this.recordBody.children[this.currentRowPos].children[0].classList.remove('focus')
        this.labelTr.children[this.currentColPos + 1].classList.remove('focus')
      }
    },
    inputBoxComplete () {
      if (this.inputBoxChanged) {
        const value = this.inputBox._value || this.inputBox.value
        this.inputBox._value = ''
        this.inputCellWrite(value)
        this.inputBoxChanged = false
      }
      this.inputBoxShow = 0
      this.showDatePicker = false
      // Without this, the cell cannot refresh, dont know why
      this.focused = false
      this.focused = true
    },

    /* *** Update *******************************************************************************************
     */
    undoTransaction (e) {
      if (e) e.preventDefault()
      if (this.redo.length === 0) return
      const transaction = this.redo.pop()
      transaction.every((t) => {
        try {
          if (t.type === 'd') {
            // deleteRecord() transaction
            this.newRecord(t.rec, false, true, true)
          }
          else if (t.field && t.field.keyField && t.oldKeys.includes(t.newVal)) {
            // newRecord() transaction
            const valueRowPos = this.value.findIndex(v => v.$id === t.$id)
            if (valueRowPos >= 0) {
              this.deleteRecord(valueRowPos, true)
              // return false
            }
          }
          else
            this.updateCell(t.$id, t.field.name, t.oldVal, true)

          return true
        }
        catch(e) {
          return false
        }
      })
    },
    newRecord (rec, selectAfterDone, noLastPage, isUndo) {
      if (typeof rec === 'undefined') rec = {}
      this.fields.map(f => {
        if (typeof rec[f.name] === 'undefined') {
          if (f.keyField)
            rec[f.name] = '§' + this.tempKey()
          else
            rec[f.name] = null
        }
      })
      const id = rec.$id || this.tempKey()
      rec.$id = id
      this.value.push(rec)
      const rowPos = this.table.push(rec) - 1
      if (selectAfterDone) this.selected[rowPos] = id
      Object.keys(rec).forEach(name => {
        const field = this.fields.find(f => f.name === name)
        if (field) this.updateCell(rec, field, rec[name], isUndo)
      })
      // this.refresh()
      if (!noLastPage) this.lazy(() => {
        this.lastPage()
        this.moveToSouthWest()
      })
      return rec
    },
    deleteSelectedRecords () {
      const rows = this.getSelectedRecords()
      if (!rows.length) return
      this.$emit('delete-selected', rows)
    },
    deleteRecord (valueRowPos, isUndo) {
      if (this.currentRowPos === valueRowPos) this.moveNorth()
      const rec = this.value.splice(valueRowPos, 1)[0]
      setTimeout(() => {
        this.lazy(rec, (buf) => {
          this.$emit('delete', buf)
          if (!isUndo) this.redo.push(buf.map(t => ({
            type: 'd',
            rec: t
          })))
        })
      }, 100)
    },
    async updateCell (row, field, newVal, isUndo) {
      switch(row.constructor.name) {
        case 'String': // $id
          row = this.value.find(r => r.$id === row) // id
          break
        case 'Number':
          row = this.table[row] // tablePos
          break
        case 'Object': // record object
          break
        default:
          throw new Error('Invalid row argument type')
      }
      switch(field.constructor.name) {
        case 'String': // field name
          field = this.fields.find(f => f.name === field)
          break
        case 'Number': // field pos
          field = this.fields[field]
          break
        case 'Object': // field object
          break
        default:
          throw new Error('Invalid field argument type')
      }
      if (!field) throw new Error('Invalid field argument')
      if (!row) return // No row is found

      const oldVal = row[field.name]
      const oldKeys = this.getKeys(row)

      if (field.change) {
        let result = await field.change(newVal, oldVal, row, field)
        if (result === false) return
      }

      row[field.name] = newVal

      setTimeout(() => {
        const transaction = {
          $id: row.$id,
          keys: this.getKeys(row),
          oldKeys: oldKeys,
          name: field.name,
          field: field,
          oldVal: typeof oldVal !== 'undefined' ? oldVal : '',
          newVal: newVal,
          err: ''
        }

        if (field.validate !== null) transaction.err = field.validate(newVal, oldVal, row, field)
        if (field.mandatory && newVal === '')
          transaction.err += (transaction.err ? '\n' : '') + field.mandatory
        this.setFieldError(transaction.err, row, field)

        if (this.validate !== null) {
          transaction.rowerr = this.validate(newVal, oldVal, row, field)
          this.setRowError(transaction.rowerr, row)
        }

        if (field.summary)
          this.calSummary(field.name)

        this.lazy(transaction, (buf) => {
          this.$emit('update', buf)
          if (!isUndo) this.redo.push(buf)
        }, 50)
      })
    },
    updateSelectedRows (field, content) {
      this.processing = true
      setTimeout(() => {
        Object.keys(this.selected).forEach(recPos => this.updateCell(parseInt(recPos), field, content))
        this.processing = false
      }, 0)
    },
    /** field.name 可能含 ':'（如 custom:num），不能用 querySelector('td#…') */
    getSystableTdByDomId (domId) {
      if (domId == null || domId === '' || !this.systable) return null
      const id = String(domId)
      const doc = this.systable.ownerDocument || document
      const byId = doc.getElementById(id)
      if (byId && byId.tagName === 'TD' && this.systable.contains(byId)) return byId
      if (typeof CSS !== 'undefined' && typeof CSS.escape === 'function') {
        return this.systable.querySelector('td#' + CSS.escape(id))
      }
      return this.systable.querySelector('td[id="' + id.replace(/\\/g, '\\\\').replace(/"/g, '\\"') + '"]')
    },
    setFieldError (error, row, field) {
      const id = `id-${row.$id}-${field.name}`
      const selector = this.getSystableTdByDomId(id)
      if (error) {
        this.errmsg[id] = error
        this.$emit('validate-error', error, row, field)
        if (selector) selector.classList.add('error')
      }
      else
      if (this.errmsg[id]) {
        delete this.errmsg[id]
        this.$emit('validate-error', '', row, field)
        if (selector) selector.classList.remove('error')
      }
    },
    setRowError (error, row) {
      const rid = `rid-${row.$id}`
      const selector = this.getSystableTdByDomId(rid)
      if (error) {
        this.rowerr[rid] = error
        this.$emit('validate-error', error, row)
        if (selector) selector.classList.add('error')
      }
      else
      if (this.rowerr[rid]) {
        delete this.rowerr[rid]
        this.$emit('validate-error', '', row)
        if (selector) selector.classList.remove('error')
      }
    },

    /* *** Autocomplete ****************************************************************************************
     */
    async calAutocompleteList (force) {
      if (!this.currentField.autocomplete) return
      if (force || (this.inputBoxChanged && this.inputBox.value.length > 0)) {
        if (typeof this.recalAutoCompleteList !== 'undefined') clearTimeout(this.recalAutoCompleteList)
        const doList = async () => {
          if (!force) {
            if (!this.focused || !this.inputBoxShow || !this.inputBoxChanged || !this.inputBox.value.length) {
              this.autocompleteInputs = []
              return
            }
          }
          const field = this.currentField
          const name = field.name
          const value = this.inputBox.value.toUpperCase()
          const listCount = this.autocompleteCount
          let list = []
          if (field.options) {
            if (field.options.constructor.name.endsWith('Function')) {
              list = await field.options(value, this.currentRecord)
              if (field.type === 'map') list = Object.values(list)
              else list = list.slice()
              if (this.inputBoxShow)
                list = list.filter(element => element.toUpperCase().includes(value))
              /* map 下拉顺序由宿主 options 插入顺序决定，勿 sort（否则「急高中低」会变成字典序） */
              if (field.type !== 'map') list.sort()
              list.splice(listCount)
              // this.autocompleteSelect = list.findIndex(element => element.toUpperCase().includes(value))
            }
            else if (Object.values(field.options).length > 0) {
              list = field.options
              if (field.type === 'map') list = Object.values(list)
              else list = list.slice()
              if (this.inputBoxShow)
                list = list.filter(element => element.toUpperCase().includes(value))
              if (field.type !== 'map') list.sort()
              list.splice(listCount)
              // this.autocompleteSelect = list.findIndex(element => element.toUpperCase().includes(value))
            }
          }
          else {
            for(let i=0; i<this.value.length; i++) {
              const rec = this.value[i]
              if (typeof rec[name] !== 'undefined' && rec[name].toString().toUpperCase().startsWith(value) && list.indexOf(rec[name]) === -1)
                list.push(rec[name])
              if (list.length >= listCount) break
            }
            list.sort()
          }
          this.autocompleteSelect = list.findIndex(element => element?.toString().toUpperCase().startsWith(value))
          this.autocompleteInputs = list
          const rect = this.currentCell.getBoundingClientRect()
          this.lazy(() => {
            if (!this.$refs.autocomplete) return
            this.$refs.autocomplete.style.minWidth = rect.width + 'px'
            const r = this.$refs.autocomplete.getBoundingClientRect()
            if (rect.bottom + r.height > window.innerHeight) {
              // show at top
              this.autocompleteInputs.reverse()
              this.autocompleteSelect = this.autocompleteInputs.length - this.autocompleteSelect - 1
              this.$refs.autocomplete.style.top = (rect.top - r.height) + 'px'
            }
            else {
              this.$refs.autocomplete.style.top = rect.bottom + 'px'
            }
            if (rect.left + r.width > window.innerWidth)
              this.$refs.autocomplete.style.left = (rect.right - r.width) + 'px'
            else
              this.$refs.autocomplete.style.left = rect.left + 'px'
            const showTop = this.autocompleteSelect * 23 - 206
            this.$refs.autocomplete.scrollTop = showTop > 0 ? showTop : 0
          })
          return this.autocompleteSelect
        }
        if (force)
          doList()
        else
          this.lazy(doList, 700)
      }
    },
    inputAutocompleteText (text, e) {
      if (e) e.preventDefault()
      this.autocompleteInputs = []
      this.autocompleteSelect = -1
      this.inputBoxShow = 0
      this.inputBoxChanged = false
      setTimeout(() => {
        this.inputCellWrite(text)
      })
    },

    /* *** Helper ****************************************************************************************
     */
    tempKey () {
      return (new Date().getTime() % 1e8) + '-' + Math.random().toString().slice(2, 7)
    },
    hashCode (s) {
      return s.split('').reduce((a, b) => {
        return a = ((a << 5) - a) + b.charCodeAt(0) | 0
      }, 0)
    },
    /** 匿名箭头函数 toString 相同会导致 lazy 回调互相覆盖，@update 等事件丢失 */
    veeLazyCallbackKey (p) {
      if (!p._veeLazyKey) {
        const seq = (this._veeLazyKeySeq = (this._veeLazyKeySeq || 0) + 1)
        Object.defineProperty(p, '_veeLazyKey', { value: 'vee-lazy-' + seq, enumerable: false })
      }
      return p._veeLazyKey
    },
    lazy (p, delay, p1) {
      if (typeof p !== 'function') return this.lazyBuf(p, delay, p1)
      if (!delay) delay = 20
      const hash = this.veeLazyCallbackKey(p)
      if (this.lazyTimeout[hash]) clearTimeout(this.lazyTimeout[hash])
      this.lazyTimeout[hash] = setTimeout(() => {
        p()
        delete this.lazyTimeout[hash]
      }, delay)
    },
    lazyBuf (item, p, delay) {
      if (!delay) delay = 20
      const hash = this.veeLazyCallbackKey(p)
      if (this.lazyBuffer[hash])
        this.lazyBuffer[hash].push(item)
      else
        this.lazyBuffer[hash] = [item]

      if (this.lazyTimeout[hash]) clearTimeout(this.lazyTimeout[hash])
      this.lazyTimeout[hash] = setTimeout(() => {
        p(this.lazyBuffer[hash])
        delete this.lazyTimeout[hash]
        delete this.lazyBuffer[hash]
      }, delay)
    }
  }
}
</script>

<style scoped>
/* 与宿主 Element 主题主色一致（element-variables.scss $--color-primary） */
.vue-excel-editor {
  --vee-primary: #1890ff;
  --vee-primary-rgb: 24, 144, 255;
}
input:focus, input:active:focus, input.active:focus {
  outline: none;
  box-shadow: inset 0 -1px 0 #ddd;
}
*, *::before, *::after {
  box-sizing: border-box;
}
.fill-drag-overlay {
  position: absolute;
  pointer-events: none;
  z-index: 2;
  box-sizing: border-box;
  border: 2px solid #1890ff;
  background: rgba(var(--vee-primary-rgb), 0.05);
}
/* 低于 input-square(4)，避免盖住活动单元格蓝框；高于 fill-drag(2) 与 systable(-1)，行选外框不被遮挡 */
.cell-range-overlay {
  position: absolute;
  pointer-events: none;
  z-index: 3;
  box-sizing: border-box;
  border: 2px solid #1890ff;
  background: rgba(var(--vee-primary-rgb), 0.07);
}
.input-square {
  position: absolute;
  padding: 0;
  z-index: 4;
  border: 2px solid #1890ff;
  /* transition: all 0.04s linear; */
}
/* 非内联编辑时让事件穿透到下方单元格，才能拖选矩形；填充柄单独开启 pointer-events */
.input-square:not(.input-square--editing) {
  pointer-events: none;
}
.input-square.input-square--editing {
  pointer-events: auto;
}
/* 拖选/多格选区：不画组件自带活动格外框，选区仅由 cell-range-overlay 表现（填充柄仍可用） */
.input-square.input-square--range-suppress:not(.input-square--editing) {
  border-color: transparent;
  background: transparent;
}
.no-transition {
  transition: none !important;
}
.autocomplete-results {
  z-index: 15;
  position: fixed;
  padding: 3px;
  margin: -1px;
  background-color: #fafcff;
  border: 1px solid #1890ff;
  /*height: fit-content;*/
  overflow-y: scroll;
  font-size: 0.88rem;
  max-width: 300px;
  max-height: 235px;
  animation: 0.3s ease 0s normal forwards 1 fadein;
}
@keyframes fadein {
  0% { opacity: 0; }
  66% { opacity: 0; }
  100% { opacity: 1; }
}
.autocomplete-result {
  list-style: none;
  text-align: left;
  padding: 4px 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: pointer
}
.autocomplete-result.select {
  background-color: rgba(var(--vee-primary-rgb), 0.08);
}
.rb-square {
  width: 9px;
  height: 9px;
  border-top: 2px solid white;
  border-left: 2px solid white;
  border-bottom: 0;
  border-right: 0;
  background-color: #1890ff;
  position: absolute;
  bottom: -3px;
  right: -2px;
  cursor: crosshair;
  pointer-events: auto;
}
.input-box {
  opacity: 0;
  font-family: inherit;
  color: inherit;
  text-shadow: inherit;
  font-size: 0.88rem;
  width: 100%;
  height: 100%;
  border: 0;
  resize: none;
  white-space: nowrap;
  overflow: hidden;
  background: white;
}
.component-content {
  display: flex;
  flex-flow: column;
  position: relative;
  max-width:fit-content;
  word-spacing: 0.02rem;
  line-height: 1.1;
  overflow: hidden;
  border: 1px solid lightgray;
}
.center-text {
  text-align: center;
}
.table-content {
  flex: 1 1 auto;
  font-size: 1rem;
  text-shadow: 0.3px 0.3px 1px #ccc;
  overflow: scroll;
  -webkit-touch-callout: none;
  -webkit-user-select: none;
  -khtml-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
  user-select: none;
  position: relative;
  width: 100%;
  scrollbar-width: none;
}
.table-content :focus {
  outline: none;
}
.table-content::-webkit-scrollbar {
  background: transparent;
  width: 0;
  height: 0;
}
.table-content.no-footer {
  border-bottom: 0;
}
.table-content.no-footer::-webkit-scrollbar {
  height: 0;
}
.table-content::-webkit-scrollbar-thumb {
  background: #eeee;
}
.table-content::-webkit-scrollbar-thumb:hover {
  background: #9999;
}
.table-col-header {
  cursor: text;
}
.systable {
  z-index: -1;
  width: fit-content;
  border: 0;
  border-collapse: separate;
  border-spacing: 0;
  /*
  margin-bottom: -1px;
  border-bottom: 1px solid lightgray;
  */
}
.systable .last-col {
  width: 12px;
}
.systable.no-number {
  margin-left: 0 !important;
}
.systable tbody tr {
  background-color: transparent;
  text-align: left;
}
.systable tr.select td {
  background-color: rgba(var(--vee-primary-rgb), 0.06) !important;
}
.systable th, .systable td {
  vertical-align: bottom;
  padding: 0.2rem 0.3rem;
  font-size: 0.88rem;
  height: 24px;
  border-top: 0;
  border-left: 0;
}
.systable th:not(:last-child) {
  border-right: 1px solid lightgray;
}
.systable tbody td {
  cursor: cell;
  white-space: nowrap;
  overflow-x: hidden;
  text-overflow: ellipsis;
  /* animation: fadein 0.2s; */
}
.systable tbody td.link {
  color: blue;
  cursor: pointer !important;
}
.systable tbody td.link:hover {
  text-decoration: underline;
}
.systable tbody td.error {
  background-image: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABIAAAASCAYAAABWzo5XAAAAAXNSR0IArs4c6QAAAAlwSFlzAAAXEgAAFxIBZ5/SUgAAAGZJREFUOBGlzjsOgDAMA9CwcQSO0PtP3K64Qyugv8S2ZMXTUw5DstmFk8qWAuhEbzSzbQ+oWIPKULAPpGAdxGJDiMGmUBRbQhFsC3kxF+TB3NAOC0ErLAzNMAoaYTT0xyTojclQxR5H5B1HhuS+WAAAAABJRU5ErkJggg==') !important;
  background-repeat: no-repeat !important;
  background-size: 8px 8px !important;
  background-position: right 0px top 0px !important;
}
.systable tbody tr:not(:last-child) td {
  border-bottom: 1px solid lightgray;
}
.systable tbody tr:last-child td {
  border-bottom: 1px solid transparent;
}
.systable td:not(:last-child) {
  border-right: 1px solid lightgray;
}
.systable thead th, .systable thead td {
  padding: 0.4rem 0.3rem;
  font-weight: 400;
  top: 0;
  height: 29px;
  position: sticky;
  z-index: 5;
  border-bottom: 1px solid lightgray;
}
.systable thead th {
  background-color: #e9ecef;
  cursor: s-resize;
  z-index: 6;
}
.systable thead td.column-filter {
  text-align: left;
  background-color: #fffff2;
  white-space: nowrap;
  overflow-x: hidden;
  text-overflow: ellipsis;
}
.systable th.focus {
  border-bottom: 1px solid #1890ff !important;
}
.systable td.first-col.focus {
  border-right: 1px solid #1890ff !important;
}
.systable tbody td.select:not(.readonly) {
  background-image: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABIAAAASCAMAAABhEH5lAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAASUExURQAAANra2tfX19ra2tnZ2dnZ2c8lDs8AAAAFdFJOUwAwQL/PKlwehgAAAAlwSFlzAAAXEQAAFxEByibzPwAAAEdJREFUKFNdyskBACAIA8F49d+yiBEh+9rHYC5poPGiDmUDUGZI2EHCHBV2UWFEiT2UWKBgHwVLiCwjsoKcVeRMkDFFxoiADdH4AyvGhvOPAAAAAElFTkSuQmCC');
  background-repeat: no-repeat;
  background-size: 8px 8px;
  background-position: right 6px center;
  vertical-align: middle;
}
.systable tbody td.datepick:not(.readonly) {
  background-image: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABIAAAASCAMAAABhEH5lAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAASUExURQAAANra2tfX19ra2tnZ2dnZ2c8lDs8AAAAFdFJOUwAwQL/PKlwehgAAAAlwSFlzAAAXEQAAFxEByibzPwAAAEdJREFUKFNdyskBACAIA8F49d+yiBEh+9rHYC5poPGiDmUDUGZI2EHCHBV2UWFEiT2UWKBgHwVLiCwjsoKcVeRMkDFFxoiADdH4AyvGhvOPAAAAAElFTkSuQmCC');
  background-repeat: no-repeat;
  background-size: 8px 8px;
  background-position: right 6px center;
  vertical-align: middle;
}
.systable .first-col {
  background: transparent;
  width: 40px;
  position: sticky;
  left: 0;
  top: auto;
  cursor: e-resize !important;
  text-overflow: inherit !important;
  text-align: center;
  padding-left: 0;
  padding-right: 0;
  overflow: hidden;
  z-index: 5;
}
.systable .sticky-column {
  position: sticky;
  z-index: 2;
}
.systable tbody td.sticky-column {
  background-color: transparent;
}
.systable thead th.sticky-column {
  z-index: 7;
}
.systable thead td.sticky-column {
  z-index: 6;
}
.systable thead td.first-col, .systable thead th.first-col {
  cursor: pointer !important;
  z-index: 10;
}
.systable tfoot .row-summary {
  height: 25px;
  border-right: 0;
  border-top: 1px solid lightgray;
  position: sticky;
  bottom: 0;
  z-index: 4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #fffff2;
}
.systable tfoot .row-summary.sticky-column {
  z-index: 5;
}
.systable tfoot .row-summary.summary-column1 {
  border-right: 1px solid lightgray;
}
.systable tfoot .row-summary.summary-column2 {
  border-right: 1px solid lightgray;
  background: white;
}
.systable tfoot .row-summary:last-child {
  border-right: 0 !important;
}
.systable tfoot .row-summary.first-col {
  height: 25px;
  border-top: 1px solid lightgray;
  border-right: 1px solid lightgray;
  background: #e9ecef;
  position: sticky;
  left: 0;
  top: auto;
  z-index: 6;
}
.footer {
  z-index: 5;
  padding: 0;
  font-size: 12px;
  color: dimgray;
  background-color: white;
  width: 100%;
  height: 25px;
  line-height: 2.3;
  border-top: 1px solid lightgray;
  user-select: none;
}
.footer .left-block {
  position: absolute;
  left: 0;
  height: 25px;
  width: 40px;
  background-color: #e9ecef;
  border-right: 1px solid lightgray;
}
.h-scroll {
  z-index: -1;
  position: absolute;
  background-color: #f4f6f9;
  height: 25px;
  margin-left: 40px;
  width: 65%;
  cursor: pointer;
}
.h-scroll:hover, .h-scroll.focus, .footer:hover .h-scroll {
  background-color: lightgray;
}

.footer a {
  cursor: pointer;
  color: #007bff;
}
.footer a.disabled {
  cursor: not-allowed;
  color: gray;
  pointer-events: none;
}
.footer a:hover {
  text-decoration: underline;
}
.v-scroll {
  display: inline-block;
  position: absolute;
  right: 0;
  width: 13px;
  z-index: 5;
  background-color: white;
  border-left: 1px solid lightgray;
  user-select: none;
}
.v-scroll-button {
  position: relative;
  display: inline-block;
  width: 100%;
  z-index: 10;
  background-color: #f4f6f9;
  cursor: pointer;
}
.v-scroll-button.focus, .v-scroll-button:hover, .v-scroll:hover .v-scroll-button {
  background-color: lightgray;
}
.runner {
  font-size: 0.88rem;
  padding: 0.5rem;
  position: absolute;
  right: 23px;
  top: 4px;
  display: inline-block;
  width: fit-content;
  z-index: 10;
  background-color: #e9ecef;
  border-radius: 3px;
  white-space: nowrap;
}
.runner:before {
  content: '';
  display: block;
  width: 0;
  height: 0;
  position: absolute;
  border-top: 4px solid transparent;
  border-bottom: 8px solid transparent;
  border-left: 8px solid #e9ecef;
  right: -8px;
  top: 4px;
}

.front-drop {
  position: fixed;
  opacity: 0.4;
  display:flex;
  justify-content:center;
  align-items:center;
  background-color: #55555555;
  color: white;
  z-index: 1000;
}
a:disabled {
  cursor: not-allowed;
  color: gray;
  pointer-events: none;
}
.col-sep {
  position: absolute;
  top: 0;
  right: 0;
  border-right: 5px solid transparent;
  width: 5px;
  cursor: col-resize;
  height: 100%;
  z-index: 15;
}
.add-col-btn {
  display: none;
  position: absolute;
  top: 12px;
  right: 8px;
  width: 20px;
  height: 20px;
  background-color: #2222;
  z-index: 15;
  border: solid 1px #2222;
  border-radius: 5px;
  cursor: pointer;
  color: white;
}
.add-col-btn:hover {
  background-color: #7777;
}
.sort-asc-sign {
  background-image: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABkAAAAZCAMAAADzN3VRAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAeUExURQAAAK+np6+qqq+rq62pqa+rq6+rq6+srK+rq6+rq2v5ERwAAAAJdFJOUwAgYHCAv8/f71KXockAAAAJcEhZcwAAFxEAABcRAcom8z8AAABNSURBVChT7clRAoAgCATRtTLq/hcuBEN0j9B8zoNV76j6s37hsh+a+NWknQ3l8pGTAk4KlAwIdVgoYKIREmUYaIaPVnBi0IjDS2cA8AC8JAq/VhDqzAAAAABJRU5ErkJggg==');
  background-repeat: no-repeat;
  background-size: 9px 9px;
  background-position: right 5px top 3px;
}
.sort-des-sign {
  background-image: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABkAAAAYCAMAAAA4a6b0AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAkUExURQAAAK+np6+qqq+rq6+pqa+qqq+rq6+rq6+rq6+srK+rq6+rqzDc2iQAAAALdFJOUwAgMEBQYHC/z9/v4u0IugAAAAlwSFlzAAAXEQAAFxEByibzPwAAAFVJREFUKFPtyVEWgCAIRFHC1HL2v98o53SEWELvC+bK0nYor5CeGCkZICUdBhkRjAoX9oLlqCzgyAOwc5caAKgT2gdIGQDNgGfM6Knzv+vcZr8kInIBUrIMxVKQeVsAAAAASUVORK5CYII=');
  background-repeat: no-repeat;
  background-size: 9px 9px;
  background-position: right 5px top 3px;
}
.filter-sign {
  background-image: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAB4AAAAlCAMAAABiU6n+AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAhUExURQAAAK+vr6+np6+qqq+pqa+qqq2pqa+srK+srK+rq6+rq01/tHwAAAAKdFJOUwAQIDBQYICv3+8SoWj4AAAACXBIWXMAABcRAAAXEQHKJvM/AAAAiUlEQVQ4T+3OOxaAMAhEUfwmsv8FazLjMRBiZ+er4ExzJelLSSZeUccisvMO2uUq8+nKU5lXfl1bWWWkS1gHuuJCoa66UKCDCwU6ulCnu13I6x4XcrrGhYyudSGjMy7U6KwLPTrvQhwDV42jaueqcVTl7+L4zzaO386Zv+teZ/6u9xXzcK2zXUVOdLIT4IImCksAAAAASUVORK5CYII=');
  background-repeat: no-repeat;
  background-size: 9px 9px;
  background-position: right 0px bottom 0px;
}
.hide {
  display: none !important;
}
@keyframes fadein {
  from {opacity: 0}
  to {opacity: 1}
}

.fa-spin {
  -webkit-animation: fa-spin 2s infinite linear;
  animation: fa-spin 2s infinite linear;
}
.svg-inline--fa.fa-w-14 {
  width: 0.875em;
}
.svg-inline--fa.fa-w-16 {
  width: 1em;
}
.svg-inline--fa.fa-fw {
  width: 1.25em;
}
.svg-inline--fa {
  display: inline-block;
  font-size: inherit;
  height: 1em;
  overflow: visible;
  vertical-align: -0.125em;
}
.fa-fw {
  text-align: center;
  width: 1.25em;
}
.fa-xs {
  font-size: 0.75em;
}
.fa-sm {
  font-size: 0.875em;
}
.fa-3x {
  font-size: 3em;
}
.tool-tip {
  display: inline-block;
  position: fixed;
  color: white;
  background-color: red;
  padding: 0.5rem;
  min-height: 1rem;
  max-width: 200px;
  word-wrap: break-word;
  border-radius: 4px;
  z-index: 50;
}
.tool-tip:before {
  content: '';
  display: block;
  width: 0;
  height: 0;
  position: absolute;
  border-top: 8px solid transparent;
  border-bottom: 8px solid transparent;
  border-right: 8px solid red;
  left: -8px;
  top: 8px;
}
.norecord {
  z-index: 1;
  font-size: smaller;
  position: absolute;
  left: 50%;
  transform: translate(-50%, 0%);
}
/* 可编辑纯文本列（无 options）：内联文本在格内上下居中；含 options 的交付物/计划列勿用此类 */
.input-square.input-square--defect-name {
  display: flex;
  flex-direction: column;
}
.input-square.input-square--defect-name > div:first-child {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: stretch;
  /* 覆盖内联 padding:2px 2px 1px：左右不再叠加，与单元格 td 正文左缘对齐 */
  padding: 2px 0 1px 0 !important;
}
.input-square.input-square--defect-name textarea.input-box {
  height: auto !important;
  max-height: 100% !important;
  width: 100% !important;
  min-height: 1.35em;
  flex: 0 1 auto;
  box-sizing: border-box;
  line-height: 1.45;
  /* 与 .systable th, .systable td 一致 */
  padding: 0.2rem 0.3rem !important;
}
/* 库内全局 input:focus 在底边画 inset 线，会把文字衬得偏上 */
.input-square.input-square--defect-name textarea.input-box:focus,
.input-square.input-square--defect-name textarea.input-box:active {
  box-shadow: none !important;
}
</style>
