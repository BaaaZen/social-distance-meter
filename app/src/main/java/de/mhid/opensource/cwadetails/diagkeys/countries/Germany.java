package de.mhid.opensource.cwadetails.diagkeys.countries;

import de.mhid.opensource.cwadetails.R;

public class Germany extends Country {
    @Override
    protected String getCountryBaseUrl() {
        return "https://svc90.main.px.t-online.de/version/v1/diagnosis-keys/country/EUR";
    }

    @Override
    public String getCountryCode() {
        return "de";
    }

    @Override
    public int getCountryName() {
        return R.string.country_de;
    }
}
