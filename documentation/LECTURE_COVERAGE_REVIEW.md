# Lecture coverage review

Reviewed against `Slides.rar`, supplied on 16 November 2026, and main through `8b2ac34`. The archive contains 15 PDFs (Weeks 1-11 and supporting layout, Material, RecyclerView and iOS documents). Lecture examples are reference material, not instructions to add every demonstrated technology.

## Explicit project requirement found

Week 1, page 25: Assignment 2 is a group project building a complete application integrating an API, a database and a security mechanism. Page 24 assigns it 30% of the module. A detailed assignment rubric was not identified in the supplied slide set; do not promise full marks from lecture-topic coverage alone.

## Coverage and meaningful next work

| Lecture | Existing evidence in app | Useful remaining verification or improvement |
|---|---|---|
| Week 4: architecture, lifecycle, MVVM | ViewModels, repository interfaces, Room DAOs, Fragments and lifecycle-owned tracking | Move story screen state into a ViewModel and preserve unfinished puzzle state through rotation/process recreation. |
| Weeks 5-6 and UI guides: navigation, Material, responsive layouts, feedback | Navigation graph, Material widgets, RecyclerView adapters, permission notices | Apply the requested UI reference while testing large fonts, small screens, touch targets, contrast, screen-reader labels and clear next actions. |
| Week 7: APIs, HTTP, JSON, errors | Firebase Auth/Firestore service layer, remote DTOs and sync worker | Demonstrate real multi-device requests and failures. Confirm whether marking requires a direct REST/JSON endpoint beyond Firebase SDK use. If so, outdoor weather before starting is a relevant optional addition; do not add an unrelated API just for a checkbox. |
| Week 8: location, maps, geofences | Location repository, distance/bearing, checkpoint geofences and map | Physically test entry/exit, inaccurate GPS, permission denial, location switched off and background restrictions. OpenStreetMap fulfils map presentation; ask only if the rubric specifically mandates Google Maps SDK. |
| Week 8: sensors and camera | Accelerometer tilt, direction guidance, CameraX QR scanning, torch and ambient-light hints | Validate jug difficulty on several phones and ensure unused sensors are stopped. Proximity is taught as a use case, not stated as a mandatory game feature. It is removed from the new story. |
| Week 9: local/cloud storage, CRUD, sync, validation | Room entities/DAOs, creator create/edit/delete, Firebase, queued sync, atomic checkpoint awards | Test offline play then reconnect, duplicate retry, two-device progress, authorization failures and conflict handling against the current Firebase project. Source presence is not proof these deployments work. |
| Week 10: unit/UI/instrumented tests and debugging | JVM tests plus device camera, sensor and Room checks | Add a repeatable full-story UI route and document physical-device, rotation and lifecycle checks. Capture profiler evidence for sensor/battery use. |
| Week 1 CLO 5 and Week 9 security | Firebase authentication, permissions, in-app privacy policy | Verify deployed rules enforce creator ownership and player isolation. Resolve guest authentication configuration and show a clear error. |
| Week 1 CLO 6: IoT/deployment; Week 11: iOS | Android implementation | These are course learning topics, not evidence that this Android group project must also ship iOS or buy IoT hardware. Confirm the actual rubric before expanding scope. |

## Recommendation

Prioritize dependable game instructions, lifecycle handling, real cloud/offline demonstrations and accessible UI. These strengthen the existing app and match the lectures. Do not add proximity, microphone recordings, Bluetooth devices, SOAP, a second database implementation or a separate iOS app solely because they are mentioned in class.

No Firebase configuration or shared data was changed for this review. Cloud correctness, release signing, store deployment and the complete outdoor route remain unverified in this pass.
