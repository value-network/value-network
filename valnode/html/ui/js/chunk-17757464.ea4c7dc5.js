(window["webpackJsonp"]=window["webpackJsonp"]||[]).push([["chunk-17757464"],{"6f5b":function(t,n,e){"use strict";var i=e("754c"),s=e.n(i);s.a},"754c":function(t,n,e){},d0dc:function(t,n,e){"use strict";e.r(n);var i=function(){var t=this,n=t.$createElement,e=t._self._c||n;return e("div",{staticClass:"create__wrap"},[e("vs-container",[t.step===t.STEPS.CREATING?e("div",{staticClass:"create"},[e("h6",{staticClass:"create__title"},[t._v(t._s(t.$t("login.creatYAccount")))]),e("div",{staticClass:"create__sub-title"},[t._v(t._s(t.$t("login.YAGPK")))]),e("div",{staticClass:"create__mnemonic d-flex j-center j-sb"},[e("div",{staticClass:"d-flex flex-wrap",staticStyle:{width:"360px"}},[e("span",{staticClass:"w-100",staticStyle:{"word-wrap":"break-word"}},[t._v(t._s(t.privatekey))])]),e("el-button",{directives:[{name:"clipboard",rawName:"v-clipboard:copy",value:t.privatekey,expression:"privatekey",arg:"copy"}],staticClass:"button——copy vs-mini-button no-border font-color__button-primary copy",attrs:{size:"mini",plain:""}},[t._v(t._s(t.$t("myaccount.copy")))])],1),e("div",{staticClass:"create__sub-title"},[t._v(t._s(t.$t("login.YAGP")))]),e("div",{staticClass:"create__mnemonic"},t._l(t.mnemonic,function(n,i){return e("span",{key:i,staticClass:"mnemonic__s"},[t._v(t._s(t.getDescription(n))+" ")])}),0),e("div",{staticClass:"create__tips"},[t._v(t._s(t.$t("login.TIPS")))]),e("div",{staticClass:"create__waring"},[t._v("\n          "+t._s(t.$t("login.attention"))+"\n        ")]),e("div",{staticClass:"next-button__wrap"},[e("el-button",{staticClass:"next-button",attrs:{type:"primary"},on:{click:t.create}},[t._v(t._s(t.$t("login.next")))])],1)]):t.step===t.STEPS.VERIFICATION?e("div",{staticClass:"create"},[e("h6",{staticClass:"create__title"},[t._v(t._s(t.$t("login.backup")))]),e("div",{staticClass:"create__sub-title"},[t._v(t._s(t.$t("login.backupTips")))]),e("div",{staticClass:"create__mnemonic"},[e("div",{staticClass:"create__mnemonic--backup"},t._l(t.verifyMnemonic,function(n,i){return e("span",{key:i,staticClass:"single__mnemonic single__mnemonic--input",on:{click:function(e){return t.removeMnemonic(n)}}},[e("i",{staticClass:"el-icon-circle-close single__mnemonic--icon"}),t._v("\n            "+t._s(t.getDescription(n))+"\n          ")])}),0)]),e("div",{staticClass:"random__mnemonic"},t._l(t.randomMnemonic,function(n,i){return e("span",{key:i,staticClass:"single__mnemonic single__mnemonic--select",class:{"single__mnemonic--disabled":t.verifyMnemonic.includes(n)},on:{click:function(e){return t.selectMnemonic(n)}}},[t._v("\n            "+t._s(t.getDescription(n))+"\n          ")])}),0),e("div",{staticClass:"next-button__wrap"},[e("el-button",{staticClass:"next-button",attrs:{type:"primary"},on:{click:t.verify}},[t._v(t._s(t.$t("login.confirm")))])],1)]):t.step===t.STEPS.CREATED?e("div",{staticClass:"result"},[e("div",{staticClass:"result-icon__wrap"},[e("vs-icon",{staticClass:"result-icon",attrs:{name:"success"}})],1),e("span",{staticClass:"result__content"},[t._v(t._s(t.$t("login.okTip")))]),e("div",{staticClass:"finish-button__wrap"},[e("el-button",{staticClass:"finish-button",attrs:{type:"primary"},on:{click:t.finished}},[t._v(t._s(t.$t("login.finished")))])],1)]):t._e()])],1)},s=[],c=e("a34a"),a=e.n(c),r=e("aff9"),o=e("5c96");function l(t,n,e,i,s,c,a){try{var r=t[c](a),o=r.value}catch(l){return void e(l)}r.done?n(o):Promise.resolve(o).then(i,s)}function u(t){return function(){var n=this,e=arguments;return new Promise(function(i,s){var c=t.apply(n,e);function a(t){l(c,i,s,a,r,"next",t)}function r(t){l(c,i,s,a,r,"throw",t)}a(void 0)})}}function p(t){return v(t)||m(t)||_()}function _(){throw new TypeError("Invalid attempt to spread non-iterable instance")}function m(t){if(Symbol.iterator in Object(t)||"[object Arguments]"===Object.prototype.toString.call(t))return Array.from(t)}function v(t){if(Array.isArray(t)){for(var n=0,e=new Array(t.length);n<t.length;n++)e[n]=t[n];return e}}var d={CREATING:"CREATING",BACKUP:"BACKUP",VERIFICATION:"VERIFICATION",CREATED:"CREATED"},f={name:"Create",data:function(){var t=r["a"].generateMnemonic(),n=t.split(/\s+/g).map(function(t){return Symbol(t)}),e=t.replace(/\s+/g," "),i=r["a"].getPrivateKey(e);return{step:d.CREATING,mnemonic:p(n),mnemonicValue:e,verifyMnemonic:[],privatekey:i,randomMnemonic:n.sort(function(t){return Math.random()-.5})}},computed:{STEPS:function(){return d}},methods:{getDescription:function(t){var n="";return n=t.description||t.toString().match(/\((.+)\)/g)[0].slice(1,-1),n},removeMnemonic:function(t){this.verifyMnemonic=this.verifyMnemonic.filter(function(n){return n!==t})},create:function(){this.step=d.VERIFICATION},backup:function(){this.step=d.VERIFICATION},selectMnemonic:function(t){this.verifyMnemonic.includes(t)||this.verifyMnemonic.push(t)},verify:function(){var t=this,n=this.verifyMnemonic.map(function(n){return t.getDescription(n)}).join("")===this.mnemonic.map(function(n){return t.getDescription(n)}).join("");n?this.step=d.CREATED:o["Message"].error("助记词不正确")},finished:function(){var t=u(a.a.mark(function t(){var n,e,i,s=this;return a.a.wrap(function(t){while(1)switch(t.prev=t.next){case 0:return this.loading=o["Loading"].service({lock:!0,text:"Loading",spinner:"el-icon-loading",background:"rgba(0, 0, 0, 0.7)"}),t.prev=1,n=r["a"].getPublicKey(this.mnemonicValue),e=r["a"].getAccountIdFromPublicKey(n),i=r["a"].createAddressByAccountId(e),t.next=7,this.$store.dispatch("updateAccount",{publicKey:n,privateKey:this.privatekey,accountId:e,address:i,mnemonic:this.mnemonicValue,isPrivateKey:!1});case 7:this.$router.push("/myaccount"),t.next=13;break;case 10:t.prev=10,t.t0=t["catch"](1),console.error(t.t0);case 13:return t.prev=13,this.$nextTick(function(){s.loading.close()}),t.finish(13);case 16:case"end":return t.stop()}},t,this,[[1,10,13,16]])}));function n(){return t.apply(this,arguments)}return n}()}},y=f,h=(e("6f5b"),e("2877")),C=Object(h["a"])(y,i,s,!1,null,"ad8c8dd6",null);n["default"]=C.exports}}]);