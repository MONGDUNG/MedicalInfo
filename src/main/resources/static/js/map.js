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
	document.querySelectorAll('#hidden-btns button, #category-btns > button[data-category]').forEach(button => {
	    button.addEventListener('click', function() {
	        // 모든 버튼에서 active 클래스 제거
	        document.querySelectorAll('#hidden-btns button, #category-btns > button[data-category]').forEach(btn => {
	            btn.classList.remove('active');
	        });

	        // 클릭된 버튼에 active 클래스 추가
	        this.classList.add('active');

	        var category = this.getAttribute('data-category');
	        var group = getGroupByCategory(category);
	        updateDeptList(group);
	    });
	});
	document.getElementById('nearbyCheckbox').addEventListener('change', function(event) {
	    if (!window.isLoggedIn) {
	        alert('로그인이 필요합니다.');
	        event.preventDefault();
	        this.checked = false;
	        return;
	    }
	    
	    if (this.checked) {
	        moveMapToUserLocation();
	        
	        // 현재 활성화된 카테고리와 진료과목 확인
	        const activeButton = document.querySelector('#category-btns button.active, #hidden-btns button.active');
	        const deptSelect = document.getElementById('deptSelect');
	        
	        if (activeButton) {
	            const category = activeButton.getAttribute('data-category');
	            
	            if (deptSelect && deptSelect.value) {
	                // 진료과목이 선택된 경우
	                fetchNearbyHospitalsByDept(category, deptSelect.value);
	            } else {
	                // 카테고리만 선택된 경우
	                fetchNearbyHospitals(category);
	            }
	        }
	    }
	});
	function moveMapToUserLocation() {
	    if (window.isLoggedIn && window.userLatitude !== 'null' && window.userLongitude !== 'null') {
	        var userPosition = new kakao.maps.LatLng(window.userLatitude, window.userLongitude);
	        map.setCenter(userPosition);
	    }
	}

	function fetchNearbyHospitalsByDept(category, dept) {
	    var center = map.getCenter();
		var checkbox = document.getElementById('nearbyCheckbox');
		    
		    // 로그인 상태이고 체크박스가 체크되어 있으면 회원 좌표 사용
		    if (window.isLoggedIn && checkbox.checked && window.userLatitude !== 'null' && window.userLongitude !== 'null') {
		        lat = window.userLatitude;
		        lng = window.userLongitude;
		    } else {
		        // 아니면 지도 중심 좌표 사용
		        var center = map.getCenter();
		        lat = center.getLat();
		        lng = center.getLng();
		    }
	    var level = map.getLevel(); // 지도레벨
	    let endpoint = `/map/medicalFacilityByDept?lat=${lat}&lng=${lng}&level=${level}&category=${category}&dept=${dept}`;

	    fetch(endpoint)
	        .then(response => response.json())
	        .then(data => {
	            console.log(data); // 데이터 확인용 로그 추가
	            listElement.innerHTML = ""; // 기존 목록 초기화
	            clearMarkers(); // 기존 마커 제거

	            data.sort((a, b) => a.distance - b.distance);
	            data.forEach(place => {
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
	
    function fetchNearbyHospitals(category) {
        var center = map.getCenter();
		var checkbox = document.getElementById('nearbyCheckbox');
		   
		   // 로그인 상태이고 체크박스가 체크되어 있으면 회원 좌표 사용
		   if (window.isLoggedIn && checkbox.checked && window.userLatitude !== 'null' && window.userLongitude !== 'null') {
		       lat = window.userLatitude;
		       lng = window.userLongitude;
		   } else {
		       // 아니면 지도 중심 좌표 사용
		       var center = map.getCenter();
		       lat = center.getLat();
		       lng = center.getLng();
		   }
        var level = map.getLevel(); // 지도레벨
        let endpoint;

        endpoint = `/map/nearbyHospitals?lat=${lat}&lng=${lng}&level=${level}&category=${category}`;

        
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

	function selectMarker(marker, place) {
	    if (selectedMarker) {
	        selectedMarker.setImage(new kakao.maps.MarkerImage(
	            "https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/marker_red.png",
	            new kakao.maps.Size(33, 36)
	        ));
	        selectedMarker.setZIndex(1); // 이전 선택된 마커를 원래 위치로 보냄
	    }

	    marker.setImage(new kakao.maps.MarkerImage(
	        "https://t1.daumcdn.net/localimg/localimages/07/mapapidoc/markerStar.png",
	        new kakao.maps.Size(33, 36)
	    ));

	    marker.setZIndex(999); // 선택된 마커를 최상위로 올림

	    selectedMarker = marker;
	    map.setCenter(marker.getPosition());

	    highlightListItem(place);
	}

	function highlightListItem(place) {
	    document.querySelectorAll('.hospital-item').forEach(item => item.classList.remove('highlight'));

	    let targetItem = Array.from(document.querySelectorAll('.hospital-item')).find(item =>
	        parseFloat(item.getAttribute('data-lat')) === parseFloat(place.LAT) &&
	        parseFloat(item.getAttribute('data-lng')) === parseFloat(place.LNG)
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
	const deptList = {
	            "A": ["내과", "신경과", "정신건강의학과", "외과", "정형외과", "신경외과", "심장혈관흉부외과", "성형외과", "마취통증의학과", "산부인과", "소아청소년과", "안과", "이비인후과", "피부과", "비뇨의학과", "영상의학과", "방사선종양학과", "병리과", "진단검사의학과", "결핵과", "재활의학과", "핵의학과", "가정의학과", "응급의학과", "직업환경의학과", "예방의학과"],
	            "B": ["치과", "구강악안면외과", "치과보철과", "치과교정과", "소아치과", "치주과", "치과보존과", "구강내과", "영상치의학과", "구강병리과", "예방치과", "통합치의학과"],
	            "C": ["한방내과", "한방부인과", "한방소아과", "한방안·이비인후·피부과", "한방신경정신과", "침구과", "한방재활의학과", "사상체질과", "한방응급"],
	            "D": ["내과", "신경과", "정신건강의학과", "외과", "정형외과", "신경외과", "심장혈관흉부외과", "성형외과", "마취통증의학과", "산부인과", "소아청소년과", "안과", "이비인후과", "피부과", "비뇨의학과", "영상의학과", "방사선종양학과", "병리과", "진단검사의학과", "결핵과", "재활의학과", "핵의학과", "가정의학과", "응급의학과", "직업환경의학과", "예방의학과", "한방내과", "한방부인과", "한방소아과", "한방안·이비인후·피부과", "한방신경정신과", "침구과", "한방재활의학과", "사상체질과", "한방응급"],
	            "E": []
	        };

	document.querySelectorAll('#hidden-btns button, #category-btns > button[data-category]').forEach(button => {
		button.addEventListener('click', function() {
		var category = this.getAttribute('data-category');
		var group = getGroupByCategory(category);
		updateDeptList(group);
		});
	});

	function getGroupByCategory(category) {
		if (["상급종합", "종합병원", "병원", "정신병원", "의원"].includes(category)) return "A";
		if (["치과병원", "치과의원"].includes(category)) return "B";
		if (["한방병원", "한의원"].includes(category)) return "C";
		if (["요양병원"].includes(category)) return "D";
		return "E";
	}

	function updateDeptList(group) {
		var deptSelect = document.createElement('select');
		deptSelect.id = 'deptSelect';
		deptSelect.innerHTML = deptList[group].map(dept => `<option value="${dept}">${dept}</option>`).join('');
		document.getElementById('deptList').innerHTML = '';
		document.getElementById('deptList').appendChild(deptSelect);

			    // 진료과목 선택 시 이벤트 리스너 추가
		deptSelect.addEventListener('change', function() {
			var selectedDept = deptSelect.value;
			var category = document.querySelector('#category-btns button.active')?.getAttribute('data-category') ||
							document.querySelector('#hidden-btns button.active')?.getAttribute('data-category');

			console.log("Selected Category:", category); // 로그 추가
			console.log("Selected Department:", selectedDept); // 로그 추가

			if (category && selectedDept) {
			fetchNearbyHospitalsByDept(category, selectedDept);
			}
		});
	}
			
	document.addEventListener("click", function(event) {
	    if (event.target.classList.contains("detail-btn")) {
	        let name = document.getElementById("modalTitle").innerText;
	        let address = document.getElementById("modalAddress").innerText;
	        let phone = document.getElementById("modalPhone").innerText;
			let category = document.getElementById("modalCategory").innerText;
			
	        // 🔥 여기서 lat, lng 값을 제대로 가져오고 있는지 확인!
	        let lat = document.getElementById("modalLat")?.innerText; 
	        let lng = document.getElementById("modalLng")?.innerText; 
            console.log("📍 위도:", lat, "📍 경도:", lng);
	        let url = `/map/hospitaldetail?name=${encodeURIComponent(name)}&address=${encodeURIComponent(address)}&phone=${encodeURIComponent(phone)}&lat=${lat}&lng=${lng}&category=${encodeURIComponent(category)}`;
	        console.log("🔗 이동할 URL:", url);
	        window.location.href = url;
	    }
	});
	
	function createListItem(place) {
	    var item = document.createElement('div');
	    item.className = 'hospital-item';
	    item.innerText = `${place.NAME || "장소"}`;
		item.setAttribute('data-lat', place.LAT);
		item.setAttribute('data-lng', place.LNG);

		item.onclick = function () {
		       document.querySelectorAll('.hospital-item').forEach(item => item.classList.remove('highlight'));
		       item.classList.add('highlight');

		       let selectedMarker = markers.find(marker => {
		           return parseFloat(marker.getPosition().getLat().toFixed(6)) === parseFloat(place.LAT.toFixed(6)) &&
		               parseFloat(marker.getPosition().getLng().toFixed(6)) === parseFloat(place.LNG.toFixed(6));
		       });

		       if (selectedMarker) {
		           selectMarker(selectedMarker, place);
		       }

	        console.log("🔍 찾은 마커:", selectedMarker);

	        if (selectedMarker) {
	            selectMarker(selectedMarker, place);
	        } else {
	            console.warn("❌ 해당 병원의 마커를 찾을 수 없음:", place.NAME);
	        }

	        // 모달 표시
	        document.getElementById("modalTitle").innerText = place.NAME || "정보 없음";
	        document.getElementById("modalAddress").innerText = place.ADDRESS || "정보 없음";
	        document.getElementById("modalPhone").innerText = place.PHONE || "정보 없음";
	        document.getElementById("modalCategory").innerText = place.CATEGORY_NAME || "정보 없음";
			document.getElementById("modalLat").innerText = place.LAT || "정보 없음";
			document.getElementById("modalLng").innerText = place.LNG || "정보 없음";
			// 리뷰 데이터 표시 (응급실과 약국 제외)
			if (place.CATEGORY_NAME !== '응급실' && place.CATEGORY_NAME !== '약국') {
			    fetch(`/map/getReviewInfo?hospitalName=${place.NAME}&address=${place.ADDRESS}`)
			        .then(response => response.json())
			        .then(data => {
			            document.getElementById("reviewCount").innerText = `리뷰수: ${data.reviewCount}`;
			            document.getElementById("averageScore").innerText = `평점: ${data.avgRating.toFixed(1)}점`;
			        })
			        .catch(error => {
			            console.error('리뷰 데이터를 가져오는 중 오류 발생:', error);
			        });
			}else {
				    // 응급실이나 약국일 경우 리뷰 관련 요소 숨김 처리
				    document.getElementById("reviewCount").innerText = '';
				    document.getElementById("averageScore").innerText = '';
				}
	        
	        let modal = document.getElementById("hospitalModal");
	        modal.style.display = "block";
	    };

	    return item;
	}

	// ❌ 뒤로가기 버튼 (모달 닫기)
	document.querySelector(".close-btn").onclick = function() {
	    document.getElementById("hospitalModal").style.display = "none";
	};

	// ✅ 상세보기 버튼 (병원 상세 페이지 이동)
	document.querySelector(".detail-btn").onclick = function() {
	    let name = document.getElementById("modalTitle").innerText;
	    let address = document.getElementById("modalAddress").innerText;
	    let phone = document.getElementById("modalPhone").innerText;
	    let category = document.getElementById("modalCategory").innerText; // 카테고리 추가

	    let url = `/map/hospitaldetail?name=${encodeURIComponent(name)}&address=${encodeURIComponent(address)}&phone=${encodeURIComponent(phone)}&category=${encodeURIComponent(category)}&lat=${document.getElementById("modalLat").innerText}&lng=${document.getElementById("modalLng").innerText}`;
	    window.location.href = url;
	};
	document.addEventListener('DOMContentLoaded', function() {
	    // URL에서 진료과목 파라미터 가져오기
	    const urlParams = new URLSearchParams(window.location.search);
	    const dept = urlParams.get('dept');
	    
	    if (dept) {
	        // 의원 카테고리 버튼 자동 클릭
	        const clinicBtn = document.querySelector('button[data-category="의원"]');
	        if (clinicBtn) {
	            clinicBtn.click();
	            
	            // 진료과 선택박스가 로드될 때까지 대기
	            const checkDeptSelect = setInterval(() => {
	                const deptSelect = document.getElementById('deptSelect');
	                if (deptSelect) {
	                    clearInterval(checkDeptSelect);
	                    deptSelect.value = dept;
	                    // 진료과 검색 실행
	                    deptSelect.dispatchEvent(new Event('change'));
	                }
	            }, 100);
	        }
	    }
	});

    window.onload = initMap;