

$(warn entering $(PWD))
QCA_BT_SCRIPT := $(PWD)/vendor/qcom/opensource/commonsys/packages/modules/Bluetooth/qva_bt.sh
$(shell chmod 777 $(QCA_BT_SCRIPT))
$(shell $(QCA_BT_SCRIPT) $(PWD))
