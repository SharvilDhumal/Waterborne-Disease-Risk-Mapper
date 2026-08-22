package com.waterborne.controller;

import com.waterborne.service.DistrictService;

/**
 * Base Controller interface for sub-view controllers in JavaFX.
 * Provides a standardized method to inject the DistrictService instance.
 */
public interface BaseController {
    /**
     * Called when the view is loaded to inject the active DistrictService instance.
     * @param service DistrictService instance
     */
    void initService(DistrictService service);
}
