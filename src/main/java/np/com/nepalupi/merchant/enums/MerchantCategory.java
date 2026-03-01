package np.com.nepalupi.merchant.enums;

/**
 * Merchant categories with ISO 18245 MCC codes.
 * Adapted for Nepal's market.
 */
public enum MerchantCategory {
    FOOD_BEVERAGE("5812"),
    GROCERY("5411"),
    RETAIL("5311"),
    CLOTHING("5651"),
    ELECTRONICS("5732"),
    PHARMACY("5912"),
    TRANSPORT("4121"),
    FUEL("5541"),
    EDUCATION("8211"),
    HEALTHCARE("8011"),
    UTILITY("4900"),
    GOVERNMENT("9211"),
    TELECOM("4812"),
    HOTEL_TOURISM("7011"),
    OTHER("5999");

    private final String mccCode;

    MerchantCategory(String mccCode) {
        this.mccCode = mccCode;
    }

    public String getMccCode() {
        return mccCode;
    }
}
