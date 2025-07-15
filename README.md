# MOCN Detector

As the merger between Vodafone and Three in the UK mobile network market completed, it became
apparent that the new entity were slowly rolling out multi-operator core network (MOCN) onto a
number of sites, both near their existing headquarters and in rural areas of Berkshire and further
across the country.

While the best nerds may be using software such as Network Signal Guru on a rooted device to detect
the presence of MOCN from signalling messages directly, many other nerds are not so lucky.

This app exists to make use of Android's built-in telephony APIs to detect the presence of MOCN and
track where it is found by its users.

The app will collect data every 10 seconds by default, which can be changed via the Settings menu.

> [!IMPORTANT]
>
> This app relies on Android's own telephony APIs. These are only as accurate as the data provided
> to them by OEM modem drivers.
>
> This app is known to not report MOCN correctly on some devices, such as Google Pixel 6, 7, 8 and 9
> series devices which do not report additional PLMNs correctly.
>
> Other devices are likely to be affected too. Please do check against a known MOCN site to ensure
> data accuracy before relying on this app.
