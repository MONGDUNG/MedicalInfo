	var map;
    var selectedMarker = null;
    var listElement = document.getElementById('hospitalList');
    var markers = []; // 마커를 저장할 배열
    function initMap() {
        map = new kakao.maps.Map(document.getElementById('map'), {
            center: new kakao.maps.LatLng(37.5665, 126.9780),
            level: 4
        });
    }
    document.getElementById('more-btn').addEventListener('click', function(event) {
        event.stopPropagation(); // 이벤트 버블링 방지
        var hiddenBtns = document.getElementById('hidden-btns');
        hiddenBtns.style.display = (hiddenBtns.style.display === 'none' || hiddenBtns.style.display === '') ? 'block' : 'none';
    });

    // 외부 클릭 시 숨겨진 버튼 닫기
    document.addEventListener('click', function(event) {
        var hiddenBtns = document.getElementById('hidden-btns');
        if (!hiddenBtns.contains(event.target) && event.target.id !== 'more-btn') {
            hiddenBtns.style.display = 'none';
        }
    });

    // 숨겨진 버튼에도 이벤트 리스너 추가
    document.querySelectorAll('#hidden-btns button').forEach(button => {
        button.addEventListener('click', function() {
            var category = this.getAttribute('data-category');
            fetchNearbyHospitals(category);
            document.getElementById('hidden-btns').style.display = 'none'; // 클릭 후 숨김 처리
        });
    });
 // 기존 버튼 이벤트 리스너
    document.querySelectorAll('#category-btns > button[data-category]').forEach(button => {
        button.addEventListener('click', function() {
            var category = this.getAttribute('data-category');
            fetchNearbyHospitals(category);
        });
    });
	
    function fetchNearbyHospitals(category) {
        var center = map.getCenter();
        var lat = center.getLat(); // 위도
        var lng = center.getLng(); // 경도
        var level = map.getLevel(); // 지도레벨
        let endpoint;
        if (category === "응급실") {
            endpoint = `/map/nearbyEmergencies?lat=${lat}&lng=${lng}&level=${level}`;
        } else if (category === "약국") {
            endpoint = `/map/nearbyPharmacies?lat=${lat}&lng=${lng}&level=${level}`;
        } else {
            endpoint = `/map/nearbyHospitals?lat=${lat}&lng=${lng}&level=${level}&category=${category}`;
        }
        
        fetch(endpoint)
            .then(response => response.json())
            .then(data => {
              //  console.log(data); // 데이터 확인용 로그 추가
                listElement.innerHTML = ""; // 기존 목록 초기화
                clearMarkers(); // 기존 마커 제거

                
                // 선택된 카테고리에 따라 데이터 필터링
                var filteredData = data.filter(place => place.CATEGORY_NAME === category);
               // console.log(filteredData);
                
                filteredData.sort((a, b) => a.distance - b.distance);
                filteredData.forEach(place => {
                    var marker = createMarker(place);
                    var listItem = createListItem(place);
                    listElement.appendChild(listItem);

                    kakao.maps.event.addListener(marker, 'click', function() {
                        selectMarker(marker, place);
                    });

                    markers.push(marker); // 마커 배열에 추가
                });
            })
            .catch(error => {
                console.error('There was a problem with the fetch operation:', error);
            });
    }

    function fetchSearchResults() {
        let keyword = document.getElementById('searchInput').value.trim();
        if (keyword === "") {
            alert('검색어를 입력하세요');
            return;
        }

        clearMarkers(); // Clear existing markers

        fetch(`/map/search/item?keyword=${encodeURIComponent(keyword)}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error('Network response was not ok ' + response.statusText);
                }
                return response.json();
            })
            .then(data => {
                updateHospitalList(data);
            })
            .catch(error => {
                console.error('There was a problem with the fetch operation:', error);
            });
    }

    function toggleNearbyHospitals() {
        var checkbox = document.getElementById('nearbyCheckbox');
        if (checkbox.checked) {
            fetchNearbyHospitals();
        } else {
            listElement.innerHTML = ""; // 목록 초기화
            clearMarkers(); // 기존 마커 제거
        }
    }

    function clearMarkers() {
        markers.forEach(marker => marker.setMap(null)); // 지도에서 마커 제거
        markers = []; // 마커 배열 초기화
    }

    function createMarker(place) {
        var position = new kakao.maps.LatLng(place.LAT, place.LNG);

        var normalImage = new kakao.maps.MarkerImage(
            "https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/marker_red.png",
            new kakao.maps.Size(33, 36)
        );

        var overImage = new kakao.maps.MarkerImage(
            "https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/marker_red.png",
            new kakao.maps.Size(40, 42)
        );

        var clickImage = new kakao.maps.MarkerImage(
            "https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/markerStar.png",
            new kakao.maps.Size(33, 36)
        );

        var marker = new kakao.maps.Marker({
            position: position,
            map: map,
            image: normalImage
        });

        var infowindow = new kakao.maps.InfoWindow({
            content: `<div style="padding:5px;">${place.NAME || "장소"}</div>`
        });

        kakao.maps.event.addListener(marker, 'mouseover', function() {
            if (selectedMarker !== marker) marker.setImage(overImage);
            infowindow.open(map, marker);
        });

        kakao.maps.event.addListener(marker, 'mouseout', function() {
            if (selectedMarker !== marker) marker.setImage(normalImage);
            infowindow.close();
        });

        return marker;
    }

    function createListItem(place) {
        var item = document.createElement('div');
        item.className = 'hospital-item';
        item.innerText = `${place.NAME || "장소"}`;

        item.onclick = function() {
        	window.location.href = `/map/hospitaldetail?name=${encodeURIComponent(place.NAME || "장소")}&address=${encodeURIComponent(place.ADDRESS)}&lat=${place.LAT}&lng=${place.LNG}&phone=${place.PHONE}`;
        };
        return item;
    }

    function selectMarker(marker, place) {
        if (selectedMarker) selectedMarker.setImage(new kakao.maps.MarkerImage(
            "https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/marker_red.png",
            new kakao.maps.Size(33, 36)
        ));

        marker.setImage(new kakao.maps.MarkerImage(
            "https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/markerStar.png",
            new kakao.maps.Size(33, 36)
        ));

        selectedMarker = marker;
        map.setCenter(marker.getPosition());

        highlightListItem(place);
    }

    function highlightListItem(place) {
        document.querySelectorAll('.hospital-item').forEach(item => item.classList.remove('highlight'));
        let targetItem = Array.from(document.querySelectorAll('.hospital-item')).find(item =>
            item.innerText.includes(place.NAME || "장소")
        );
        if (targetItem) {
            targetItem.classList.add('highlight');
            targetItem.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
    }

    function openKakaoDirections() {
        window.open("https://map.kakao.com", "_blank");
    }
    function updateHospitalList(data) {
        listElement.innerHTML = ""; // 기존 목록 초기화

        if (data.length > 0) {
            var firstPlace = data[0];
            var position = new kakao.maps.LatLng(firstPlace.LAT, firstPlace.LNG);
            map.setCenter(position);
        }

        data.forEach(place => {
            var marker = createMarker(place);
            var listItem = createListItem(place);
            listElement.appendChild(listItem);

            kakao.maps.event.addListener(marker, 'click', function() {
                selectMarker(marker, place);
            });

            markers.push(marker); // 마커 배열에 추가
        });
    }
    window.onload = initMap;