(window["webpackJsonp"]=window["webpackJsonp"]||[]).push([["chunk-8a2bbbb8"],{"174e":function(t,a,s){"use strict";var o=s("f341"),e=s.n(o);e.a},2275:function(t,a,s){"use strict";var o=s("e4ff"),e=s.n(o);e.a},"5afd":function(t,a,s){"use strict";var o=s("e448"),e=s.n(o);e.a},"68ab":function(t,a,s){"use strict";var o=function(){var t=this,a=t.$createElement,s=t._self._c||a;return s("div",{staticClass:"myaccount-transitions"},[s("div",{staticClass:"transitions__header"},[s("div",{staticClass:"header__left"},[t.title?s("h6",[t._v(t._s(t.$t("blockTran.Transaction")))]):t._e(),s("i18n",{attrs:{path:"myaccount.totalTxs",tag:"span"}},[s("span",{attrs:{place:"total"}},[t._v(t._s(t.total))])])],1),s("div",{staticClass:"header__right hidden-xs-only"},[s("el-pagination",{attrs:{background:"","current-page":t.page,"page-size":t.limit,total:t.total,layout:"prev, pager, next, sizes, jumper"},on:{"update:currentPage":function(a){t.page=a},"update:current-page":function(a){t.page=a},"update:pageSize":function(a){t.limit=a},"update:page-size":function(a){t.limit=a},"size-change":t.handleChangeCurrent,"current-change":t.handleChangeCurrent}})],1)]),s("div",{staticClass:"transitions__body"},[s("div",{staticClass:"transition"},[s("el-table",{directives:[{name:"loading",rawName:"v-loading",value:t.loading,expression:"loading"}],staticClass:"transition__body",staticStyle:{},attrs:{data:t.transactionList,"span-method":t.arraySpanMethod,"row-class-name":t.addRowClassName}},[s("el-table-column",{attrs:{"show-overflow-tooltip":!0,prop:"fullHash",align:"left",label:t.$t("myaccount.hash"),width:"120"},scopedSlots:t._u([{key:"default",fn:function(a){return["TIME"===a.row.rowType?[s("div",{staticClass:"time__wrap"},[s("span",{staticClass:"font-color__secondary"},[t._v(t._s(t.$t("blockTran.Time"))+"：")]),s("span",{staticClass:"font-color__long"},[t._v(t._s(t._f("vmoment")(a.row.blockTimestamp)))])])]:[s("div",{staticClass:"row__thead"},[t._v(t._s(t.$t("myaccount.hash")))]),a.row.unconfirmedTransactions?s("span",{staticClass:"pending"},[t._v(t._s(a.row.fullHash))]):s("router-link",{attrs:{to:"/tx/"+a.row.transaction}},[s("span",{staticClass:"color-text-button-primary"},[t._v(t._s(a.row.fullHash))])])]]}}])}),s("el-table-column",{attrs:{"show-overflow-tooltip":!0,prop:"feeNQT",align:"center",label:t.$t("blockTran.Fee")},scopedSlots:t._u([{key:"default",fn:function(a){return["TIME"===a.row.rowType?s("span",[s("div",{staticClass:"thead__wrap"},[s("span",{staticClass:"font-color__secondary"},[t._v(t._s(t.$t("blockTran.Fee"))+"：")]),t._v("\n                  "+t._s(t._f("toThousands")(t._f("formatVol")(a.row.feeNQT)))+" VAL\n                ")])]):[s("div",{staticClass:"row__thead"},[t._v(t._s(t.$t("blockTran.TransactionType")))]),s("span",[t._v(" "+t._s(a.row.txType))])]]}}])}),s("el-table-column",{attrs:{"show-overflow-tooltip":!0,prop:"block",align:"center",label:t.$t("blockTran.Block")},scopedSlots:t._u([{key:"default",fn:function(a){return["TIME"===a.row.rowType?[s("div",{staticClass:"thead__wrap"},[s("span",{staticClass:"font-color__secondary"},[t._v(t._s(t.$t("blockTran.Fee"))+"：")]),t._v("\n                  "+t._s(t._f("toThousands")(t._f("formatVol")(a.row.feeNQT)))+" VAL\n                ")])]:[s("div",{staticClass:"row__thead"},[t._v(t._s(t.$t("blockTran.Block")))]),a.row.unconfirmedTransactions?s("span",{staticClass:"pending"},[t._v("(pending)")]):s("router-link",{attrs:{to:"/block/"+a.row.block}},[s("span",{staticClass:"color-text-button-primary"},[t._v(t._s(a.row.block))])])]]}}])}),s("el-table-column",{attrs:{"show-overflow-tooltip":!0,prop:"confirmations",align:"center",width:"80",label:t.$t("blockTran.Confirmations")},scopedSlots:t._u([{key:"default",fn:function(a){return["TIME"===a.row.rowType?s("span"):[s("div",{staticClass:"row__thead"},[t._v(t._s(t.$t("blockTran.Confirmations")))]),a.row.unconfirmedTransactions?s("span",[t._v("未确认")]):s("span",[t._v(t._s(a.row.confirmations))])]]}}])}),s("el-table-column",{attrs:{prop:"senderRS",align:"center",width:"240",label:t.$t("blockTran.Sender")},scopedSlots:t._u([{key:"default",fn:function(a){return["TIME"===a.row.rowType?s("span"):[s("div",{staticClass:"row__thead"},[t._v(t._s(t.$t("blockTran.Sender")))]),s("div",[[[a.row.senderRS,a.row.sender].includes(t.account)?s("span",[t._v(t._s(a.row.senderRS))]):s("router-link",{attrs:{to:"/account/"+a.row.senderRS}},[s("span",{staticClass:"color-text-button-primary"},[t._v(t._s(a.row.senderRS))])])]],2)]]}}])}),s("el-table-column",{attrs:{prop:"recipientRS",align:"center",width:"300",label:t.$t("blockTran.Recipient")},scopedSlots:t._u([{key:"default",fn:function(a){return["TIME"===a.row.rowType?s("span"):[s("div",{staticClass:"row__thead"},[t._v(t._s(t.$t("blockTran.Recipient")))]),a.row.recipient||a.row.attachment.recipients?t._e():s("div",[t._v("-")]),[1,2].includes(a.row.subtype)?s("div",[t._l(a.row.recipients,function(o,e){return s("div",{key:o.accountRS,staticClass:"address__wrap"},[a.row.showAll||e<t.limit_recipient?[[o.accountRS,o].includes(t.account)?s("span",[t._v(t._s(o.accountRS))]):s("router-link",{attrs:{to:"/account/"+o.accountRS}},[s("span",{staticClass:"color-text-button-primary"},[t._v(t._s(o.accountRS))])])]:t._e()],2)}),s("div",{staticClass:"button-more__wrap"},[a.row.recipients.length>t.limit_recipient?s("el-button",{attrs:{size:"mini",plain:""},on:{click:function(t){a.row.showAll=!a.row.showAll}}},[t._v("\n                      "+t._s(a.row.showAll?"隐藏":"更多")+"\n                      "),s("i",{staticClass:"el-icon-arrow-down"})]):t._e()],1)],2):[[a.row.recipientRS,a.row.recipient].includes(t.account)?s("span",{staticClass:"address__wrap"},[t._v(t._s(a.row.recipientRS))]):s("router-link",{staticClass:"address__wrap",attrs:{to:"/account/"+a.row.recipientRS}},[s("span",{staticClass:"color-text-button-primary"},[t._v(t._s(a.row.recipientRS))])])],s("vs-icon",{staticClass:"vs-icon-right",attrs:{name:"to"}})]]}}])}),s("el-table-column",{attrs:{"show-overflow-tooltip":!0,prop:"amountNQT",align:"right",label:t.$t("blockTran.Amount")},scopedSlots:t._u([{key:"default",fn:function(a){return["TIME"===a.row.rowType?s("span"):[s("div",{staticClass:"row__thead"},[t._v(t._s(t.$t("blockTran.Amount")))]),1==a.row.type&&2==a.row.subtype?s("span",[t._v("-")]):t._e(),[1,2].includes(a.row.subtype)?s("div",t._l(a.row.recipients,function(a){return s("div",{key:a.accountRS},[s("span",[t._v(t._s(t._f("toThousand")(t._f("toFixed1")(t._f("formatVol")(a.amountNQT))))+" VAL")])])}),0):s("span",[t._v(t._s(t._f("toThousand")(t._f("toFixed1")(t._f("formatVol")(a.row.amountNQT))))+" VAL")])]]}}])})],1)],1)]),s("el-pagination",{staticClass:"hidden-sm-and-up",attrs:{background:"",small:!0,"current-page":t.page,"page-size":t.limit,total:t.total,"pager-count":5,layout:"prev, pager, next, jumper"},on:{"update:currentPage":function(a){t.page=a},"update:current-page":function(a){t.page=a},"update:pageSize":function(a){t.limit=a},"update:page-size":function(a){t.limit=a},"size-change":t.handleChangeCurrent,"current-change":t.handleChangeCurrent}})],1)},e=[],n=s("365c"),i=s("f195"),c=s("aff9"),r=s("cf45"),l=s("07a4"),d={name:"MyAccountTransitions",props:{title:{type:Boolean,default:function(){return!0}},account:{type:String,required:!1},block:{type:String,required:!1}},data:function(){return{transactionList:[],loading:!1,total:0,limit:20,page:1}},computed:{limit_recipient:function(){return 4}},created:function(){var t=this;this.$store.state.accountOverview.isActive&&"MyAccount"===this.$route.name?(l["a"].dispatch("getAccountOverview"),this.getTransactionList()):"MyAccount"!==this.$route.name&&this.getTransactionList(),i["a"].$on("quickRefresh",function(){"MyAccount"!==t.$route.name&&t.getTransactionList(!0)}),i["a"].$on("updateTransactionList",function(){t.$store.state.accountOverview.isActive&&"MyAccount"===t.$route.name&&t.getTransactionList(!0)})},beforeDestroy:function(){i["a"].$off("updateTransactionList"),i["a"].$off("quickRefresh")},methods:{getTransactionList:function(t){var a=this;t||(this.loading=!0);var s={page:this.page,limit:this.limit};this.account&&(s["account"]=this.account),this.block&&(s["block"]=this.block),Object(n["n"])(s).then(function(t){var s=t.data;s=void 0===s?{}:s;var o=s.data,e=o.data,n=void 0===e?[]:e,i=o.total,l=void 0===i?0:i;n=Object(r["c"])(n,a.$i18n.locale),a.transactionList=n.map(function(t){if(t.showAll=!1,1===t.subtype&&(t.recipients=t.attachment.recipients.map(function(t){return{accountId:t[0],accountRS:c["a"].createAddressByAccountId(t[0]),amountNQT:t[1]}})),2===t.subtype)if(t.attachment.recipients){var a=t.amountNQT/t.attachment.recipients.length;t.recipients=t.attachment.recipients.map(function(t){return{accountId:t,accountRS:c["a"].createAddressByAccountId(t),amountNQT:a}})}else t.recipients=[];return t}).reduce(function(t,a){return t.push({rowType:"TIME",blockTimestamp:a.blockTimestamp||a.timestamp,type:a.type,feeNQT:a.feeNQT}),t.push(a),t},[]),a.total=l}).finally(function(){a.loading=!1})},handleChangeCurrent:function(t){this.getTransactionList()},arraySpanMethod:function(t){var a=t.row,s=(t.column,t.rowIndex,t.columnIndex);if("TIME"===a.rowType){if(0===s)return[1,4];if(1===s)return[1,3];if(2===s)return[0,0]}},addRowClassName:function(t){var a=t.row;return"TIME"===a.rowType?"row__time":"THEAD"===a.rowType?"row__thead":""}},watch:{account:function(){this.getTransactionList()},block:function(){this.getTransactionList()}}},u=d,p=(s("174e"),s("2877")),_=Object(p["a"])(u,o,e,!1,null,"92e86a9c",null);a["a"]=_.exports},c13e:function(t,a,s){"use strict";var o=function(){var t=this,a=t.$createElement,s=t._self._c||a;return s("div",{staticClass:"received"},[s("span",{staticClass:"received-title",attrs:{text:t.topTitle}},[t._v(t._s(t.topTitle))]),s("div",{staticClass:"received-qrcode__wrap"},[s("vs-qrcode",{attrs:{text:t.address}})],1),s("span",{staticClass:"received-address"},[t._v(t._s(t.address))]),s("el-button",{directives:[{name:"clipboard",rawName:"v-clipboard:copy",value:t.address,expression:"address",arg:"copy"}],staticClass:"received-button",attrs:{type:"text"}},[t._v(t._s(t.$t("myaccount.copyAddress")))])],1)},e=[],n={name:"Received",props:{address:{type:String,required:!0},topTitle:{type:String,required:!1}}},i=n,c=(s("5afd"),s("2877")),r=Object(c["a"])(i,o,e,!1,null,"38d24f10",null);a["a"]=r.exports},e448:function(t,a,s){},e4ff:function(t,a,s){},eb5c:function(t,a,s){"use strict";s.r(a);var o=function(){var t=this,a=t.$createElement,s=t._self._c||a;return s("div",{staticClass:"account-details"},[s("vs-container",{staticStyle:{"margin-top":"20px"}},[s("div",{staticClass:"header"},[s("h3",{staticClass:"title"},[t._v(t._s(t.$t("account.Address")))])]),s("vs-main",{directives:[{name:"loading",rawName:"v-loading",value:t.loading,expression:"loading"}],staticStyle:{padding:"0 20px","margin-top":"6px"}},[s("el-row",{attrs:{gutter:50}},[s("el-col",{attrs:{xs:24,sm:12,md:12,lg:12}},[s("div",{staticClass:"col-line"},[s("div",{staticClass:"col-left"},[t._v(t._s(t.$t("account.Address"))+"：")]),s("div",{staticClass:"col-right"},[s("span",{staticClass:"addr"},[t._v(t._s(t._f("toBlank")(t.rows.accountRS))+"\n                "),s("el-button",{directives:[{name:"clipboard",rawName:"v-clipboard:copy",value:t.rows.accountRS,expression:"rows.accountRS",arg:"copy"},{name:"clipboard",rawName:"v-clipboard:success",value:t.onCopy,expression:"onCopy",arg:"success"},{name:"clipboard",rawName:"v-clipboard:error",value:t.onError,expression:"onError",arg:"error"}],staticClass:"button——copy vs-mini-button no-border font-color__button-primary copy hidden-sm-and-down",attrs:{size:"mini",plain:""}},[t._v("\n                "+t._s(t.$t("myaccount.copy"))+"\n              ")]),s("vs-icon",{staticClass:"vs-qr-code",attrs:{name:"vs_qr_code"},nativeOn:{click:function(a){return t.handleQRCode(a)}}})],1)])])]),s("el-col",{attrs:{xs:24,sm:12,md:12,lg:12}},[s("div",{staticClass:"col-line"},[s("div",{staticClass:"col-left"},[t._v(t._s(t.$t("account.Transfers")))]),s("div",{staticClass:"col-right"},[s("div",[t._v(t._s(t.rows.totalTnx||0)),s("span",{staticClass:"font--small"},[t._v("（"),s("span",{staticClass:"number__inner"},[t._v(t._s(t.rows.tnxIn||0)),s("vs-icon",{staticClass:"icon-zoom icon-zoom__down",attrs:{name:"down"}}),t._v(t._s(t.rows.tnxOut||0)),s("vs-icon",{staticClass:"icon-zoom icon-zoom__up",attrs:{name:"up"}})],1),t._v("）")])])])])]),s("el-col",{attrs:{xs:24,sm:12,md:12,lg:12}},[s("div",{staticClass:"col-line"},[s("div",{staticClass:"col-left"},[t._v(t._s(t.$t("account.Name"))+"：")]),s("div",{staticClass:"col-right"},[t._v(t._s(t._f("toBlank")(t.rows.name)))])])]),s("el-col",{attrs:{xs:24,sm:12,md:12,lg:12}},[s("div",{staticClass:"col-line"},[s("div",{staticClass:"col-left"},[t._v(t._s(t.$t("account.Pool"))+"：")]),t.rows.minePoolRS?s("router-link",{staticClass:" color-text-button-primary",attrs:{to:"/account/"+t.rows.minePool}},[t._v("\n                "+t._s(t.rows.minePoolRS)+"\n              ")]):s("span",[t._v(t._s(t.rows.minePoolRS||"-"))])],1)]),s("el-col",{attrs:{xs:24,sm:12,md:12,lg:12}},[s("div",{staticClass:"col-line"},[s("div",{staticClass:"col-left"},[t._v(t._s(t.$t("account.TotalBalance"))+"：")]),s("div",{staticClass:"col-right"},[t._v(t._s(t._f("toThousand")(t._f("formatVol")(t.rows.totalBalance)))+" VAL")])])]),s("el-col",{attrs:{xs:24,sm:12,md:12,lg:12}},[s("div",{staticClass:"col-line"},[s("div",{staticClass:"col-left"},[t._v(t._s(t.$t("account.BlockForged"))+"：")]),s("div",{staticClass:"col-right theme-color"},[t._v("\n              "+t._s(t.rows.blockNum)+"\n            ")])])]),s("el-col",{attrs:{xs:24,sm:12,md:12,lg:12}},[s("div",{staticClass:"col-line"},[s("div",{staticClass:"col-left"},[t._v(t._s(t.$t("account.Available"))+"：")]),s("div",{staticClass:"col-right theme-color"},[t._v(t._s(t._f("toThousand")(t._f("formatVol")(t.rows.balanceNQT)))+" VAL")])])]),s("el-col",{attrs:{xs:24,sm:12,md:12,lg:12}},[s("div",{staticClass:"col-line"},[s("div",{staticClass:"col-left"},[t._v(t._s(t.$t("account.BlockEarning"))+"：")]),s("div",{staticClass:"col-right theme-color"},[t._v(t._s(t._f("toThousand")(t._f("formatVol")(t.rows.forgedBalanceNQT)))+" VAL")])])]),s("el-col",{attrs:{xs:24,sm:12,md:12,lg:12}},[s("div",{staticClass:"col-line"},[s("div",{staticClass:"col-left"},[t._v(t._s(t.$t("account.Staked"))+"：")]),s("div",{staticClass:"col-right theme-color"},[t._v(t._s(t._f("toThousand")(t._f("formatVol")(t.rows.totalPledged)))+" VAL")])])]),s("el-col",{attrs:{xs:24,sm:12,md:12,lg:12}},[s("div",{staticClass:"col-line"},[s("div",{staticClass:"col-left"},[t._v(t._s(t.$t("myaccount.stakingEarning"))+"：")]),s("div",{staticClass:"col-right theme-color"},[t._v(t._s(t._f("toThousand")(t._f("formatVol")(t.rows.pledgeRewardBalanceNQT)))+" VAL")])])]),s("el-col",{attrs:{xs:24,sm:12,md:12,lg:12}},[s("div",{staticClass:"col-line"},[s("div",{staticClass:"col-left"},[t._v(t._s(t.$t("account.Unstaking"))+"：")]),s("div",{staticClass:"col-right"},[t._v(t._s(t._f("toThousand")(t._f("formatVol")(t.rows.totalUnpledged)))+" VAL")])])]),s("el-col",{attrs:{xs:24,sm:12,md:12,lg:12}},[s("div",{staticClass:"col-line"},[s("div",{staticClass:"col-left"},[t._v(t._s(t.$t("Pool.Total"))+"：")]),s("div",{staticClass:"col-right theme-color"},[t._v(t._s(t._f("toThousand")(t._f("formatVol")(t.totalReward)))+" VAL")])])])],1)],1),s("vs-main",{staticClass:"vs-main transition--wrap"},[s("MyAccountTransitions",{attrs:{account:t.$route.params.accountId}})],1),s("el-dialog",{attrs:{width:t.pledgeDialogWidth,visible:t.isShowPledgeDialog,"show-close":!1,"destroy-on-close":!0,"custom-class":"vs-dialog"},on:{"update:visible":function(a){t.isShowPledgeDialog=a}}},[s(t.currentPledgeComponent,{tag:"component",attrs:{address:t.rows.accountRS,topTitle:t.$t("account.QrCode")},model:{value:t.isShowPledgeDialog,callback:function(a){t.isShowPledgeDialog=a},expression:"isShowPledgeDialog"}})],1)],1)],1)},e=[],n=s("365c"),i=s("c13e"),c=s("51e6"),r=s("68ab"),l={Received:"Received"},d={name:"AccountDetails",components:{MyAccountTransitions:r["a"],Received:i["a"]},data:function(){return{loading:!1,isShowPledgeDialog:!1,currentPledgeComponent:l.Received,limit:10,page:1,total:0,totalReward:0,rows:{},accountRS:"",poolsConfig:{},poolsConfigIds:{}}},computed:{isMobile:function(){return Object(c["a"])()},pledgeDialogWidth:function(){return Object(c["a"])()?"100%":[l.Received].includes(this.currentPledgeComponent)?"380px":"466px"}},created:function(){this.getAccountList(this.$route.params.accountId)},beforeRouteUpdate:function(t,a,s){this.getAccountList(t.params.accountId),s()},methods:{onCopy:function(t){this.$message({message:this.$t("myaccount.copySuccess")+":"+t.text,type:"success"})},onError:function(t){this.$message.error("复制失败")},getAccountList:function(t){var a=this;this.loading=!0,Object(n["d"])({account:t,includeTransactions:!0}).then(function(t){var s=t.data;s=void 0===s?{}:s;var o=s.data.data,e=void 0===o?[]:o;e.length&&(a.rows=e[0],a.totalReward=parseFloat(e[0].pledgeRewardBalanceNQT)+parseFloat(e[0].forgedBalanceNQT))}).finally(function(){a.loading=!1})},handleQRCode:function(){this.isShowPledgeDialog=!0}}},u=d,p=(s("2275"),s("2877")),_=Object(p["a"])(u,o,e,!1,null,"f807a7be",null);a["default"]=_.exports},f341:function(t,a,s){}}]);