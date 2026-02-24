import axios from 'axios'
import { Message } from 'element-ui'
import { saveAs } from 'file-saver'
import { getToken } from '@/utils/auth'
import errorCode from '@/utils/errorCode'
import { blobValidate } from "@/utils/ruoyi";
import {setHeader} from "@/utils/request";

const baseURL = process.env.VUE_APP_BASE_API

export default {
  name(name, isDelete = true) {
    let url = baseURL + "/common/download?fileName=" + encodeURIComponent(name) + "&delete=" + isDelete
    let headers = {};
    setHeader('/common/download', headers);
    axios({
      method: 'get',
      url: url,
      responseType: 'blob',
      headers: headers
    }).then((res) => {
      const isBlob = blobValidate(res.data);
      if (isBlob) {
        const blob = new Blob([res.data])
        this.saveAs(blob, decodeURIComponent(res.headers['download-filename']))
      } else {
        this.printErrMsg(res.data);
      }
    })
  },
  resource(resource) {
    let url = baseURL + "/common/download/resource?resource=" + encodeURIComponent(resource);
    let headers = {};
    setHeader('/common/download/resource', headers);
    axios({
      method: 'get',
      url: url,
      responseType: 'blob',
      headers: headers
    }).then((res) => {
      const isBlob = blobValidate(res.data);
      if (isBlob) {
        const blob = new Blob([res.data])
        this.saveAs(blob, decodeURIComponent(res.headers['download-filename']))
      } else {
        this.printErrMsg(res.data);
      }
    })
  },
  zip(_url, name) {
    let url = baseURL + _url
    let headers = {};
    setHeader(url, headers);
    axios({
      method: 'get',
      url: url,
      responseType: 'blob',
      headers: headers
    }).then((res) => {
      const isBlob = blobValidate(res.data);
      if (isBlob) {
        const blob = new Blob([res.data], { type: 'application/zip' })
        this.saveAs(blob, name)
      } else {
        this.printErrMsg(res.data);
      }
    })
  },
  saveAs(text, name, opts) {
    saveAs(text, name, opts);
  },
  async printErrMsg(data) {
    const resText = await data.text();
    const rspObj = JSON.parse(resText);
    const errMsg = errorCode[rspObj.code] || rspObj.msg || errorCode['default']
    Message.error(errMsg);
  }
}

